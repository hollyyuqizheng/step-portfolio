// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public final class FindMeetingQuery {

  // Constants for 0 and the number of minutes in a day. 
  // 0 means 0 minute since start of a day. 
  // 1440 is the total number of minutes in a day, which marks the end of a day. 
  private static final int START_OF_DAY_MINUTES = 0; 
  private static final int END_OF_DAY_MINUTES = 24 * 60; 

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    Collection<String> mandatoryAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees(); 
    long meetingDuration = request.getDuration(); 

    List<TimeRange> mandatoryAttendeesBusyTimeRanges = getAllBusyTimeRanges(events, mandatoryAttendees); 
    List<TimeRange> optionalAttendeesBusyTimeRanges = getAllBusyTimeRanges(events, optionalAttendees);

    sortBusyTimeRangesByStart(mandatoryAttendeesBusyTimeRanges); 
    sortBusyTimeRangesByStart(optionalAttendeesBusyTimeRanges);

    List<TimeRange> mandatoryAttendeesEmptyRanges = getEmptyTimeRanges(mandatoryAttendeesBusyTimeRanges, meetingDuration); 
    List<TimeRange> optionalAttendeesEmptyRanges = getEmptyTimeRanges(optionalAttendeesBusyTimeRanges, meetingDuration);

    if (mandatoryAttendees.isEmpty()) {
      return optionalAttendeesEmptyRanges; 
    } else if (optionalAttendees.isEmpty()) {
      return mandatoryAttendeesEmptyRanges; 
    } else {
      List<TimeRange> allEmptyTimeRanges = findCommonEmptyTimeRanges(
          mandatoryAttendeesEmptyRanges, optionalAttendeesEmptyRanges, meetingDuration);
      return allEmptyTimeRanges;
    }
  }

  /** 
   * Constructs all the time ranges that any of the mandatory attendees is busy.
   * These time ranges cannot be part of the potential meeting time. 
   */ 
  private List<TimeRange> getAllBusyTimeRanges(Collection<Event> events, Collection<String> mandatoryAttendees) {
    List<TimeRange> busyTimes = new ArrayList<TimeRange>();

    events.forEach((event) -> {
      Collection<String> eventAttendees = event.getAttendees();

      for (String attendee : eventAttendees) {
        // If the current event's attendee list contains a mandatory attendee, 
        // adds this event's time range to the set of all busy times, 
        // after checking for overlapping time ranges. 
        if (mandatoryAttendees.contains(attendee)) {
          TimeRange currEventTimeRange = event.getWhen(); 
        
          handleOverlapTimeRange(currEventTimeRange, busyTimes);

          // If any one of the mandatory attendees is in this event's guest list,
          // this event's time range needs to be considered busy. 
          // Breaks out of the for-each loop because there is no need to look
          // at the rest of the event's guest list. 
          break; 
        }
      }
    }); 
    return busyTimes; 
  }

  /**
   * Compares a given event's time slot with all the existing busy time slots.
   * Merges the current event time slot with a busy time slot if they overlap.
   * Adds the current event's time slot to the list of busy slots in the end. 
   * @param current event's time range; a collection of busy time slots
   */ 
  private void handleOverlapTimeRange(TimeRange currEventTimeRange, Collection<TimeRange> busyTimes) {
    boolean overlapped = false; 

    for (TimeRange busyTimeRange : busyTimes) {
      if (currEventTimeRange.overlaps(busyTimeRange)) {
        TimeRange combinedTimeRange = getCombinedTimeRange(currEventTimeRange, busyTimeRange);
        busyTimes.remove(busyTimeRange);
        busyTimes.add(combinedTimeRange);
        overlapped = true;  
        currEventTimeRange = combinedTimeRange; 
      } 
    }

    // If the current event doesn't overlap with any existing busy time range,
    // add the current event's time range to the set of all busy time slots. 
    if (!overlapped) {
      busyTimes.add(currEventTimeRange); 
    }
  }

  /** Creates a new overall combined time range for two time ranges that overlap. */ 
  private TimeRange getCombinedTimeRange(TimeRange a, TimeRange b) {
    int aStart = a.start();
    int aEnd = a.end();
    int bStart = b.start();
    int bEnd = b.end();

    TimeRange combinedTimeRange = TimeRange.fromStartEnd(
        /* start time = */ Math.min(aStart, bStart), 
        /* end time = */ Math.max(aEnd, bEnd),
        /* inclusive of end time = */ false); 

    return combinedTimeRange; 
  }

  /** Sorts a list of busy time ranges by their start time. */
  private void sortBusyTimeRangesByStart(List<TimeRange> allBusyTimeRanges) {
    Collections.sort(allBusyTimeRanges, TimeRange.ORDER_BY_START); 
  }

  /**
   * Finds all empty time ranges based on a list of busy time ranges and 
   * the length of the requested meeting
   */ 
  private List<TimeRange> getEmptyTimeRanges(List<TimeRange> allBusyTimeRanges, long meetingDuration) {
    List<TimeRange> emptyTimeRanges = new ArrayList<TimeRange>();

    if (allBusyTimeRanges.size() == 0 && meetingDuration <= END_OF_DAY_MINUTES) {
      emptyTimeRanges.add(TimeRange.fromStartEnd(
          /* start time = */ 0, 
          /* end time = */ END_OF_DAY_MINUTES, 
          /* inclusive of end time = */ false));
      return emptyTimeRanges; 
    }

    // A for loop is necessary because each consecutive pair 
    // of time ranges needs to be looked at at each iteration. 
    for (int i = 0; i < allBusyTimeRanges.size(); i++) {
      if (i == 0) {
        int earliestStart = allBusyTimeRanges.get(i).start();
        if (earliestStart >= meetingDuration) {
          emptyTimeRanges.add(TimeRange.fromStartEnd(
            /* start time = */ START_OF_DAY_MINUTES, 
            /* end time = */ earliestStart, 
            /* inclusive of end time = */ false)); 
        }
      } 

      if (i == allBusyTimeRanges.size() - 1) {
        int latestEnd = allBusyTimeRanges.get(i).end();
        if ((END_OF_DAY_MINUTES - latestEnd) >= meetingDuration) {
          emptyTimeRanges.add(TimeRange.fromStartEnd(
            /* start time = */ latestEnd, 
            /* end time = */ END_OF_DAY_MINUTES, 
            /* inclusive of end time = */ false)); 
        }
      } else {
        int currEnd = allBusyTimeRanges.get(i).end();
        int nextStart = allBusyTimeRanges.get(i + 1).start(); 

        if (nextStart - currEnd >= meetingDuration) {
          emptyTimeRanges.add(TimeRange.fromStartEnd(
            /* start time = */ currEnd, 
            /* end time = */ nextStart, 
            /* inclusive of end time = */ false)); 
        }
      }
    } 
    return emptyTimeRanges; 
  }

  /** Finds the common time range between time ranges of two collections */ 
  private List<TimeRange> findCommonEmptyTimeRanges(
      List<TimeRange> mandatoryAttendeesEmptyTimeRanges, 
      List<TimeRange> optionalAttendeesEmptyTimeRanges, 
      long meetingDuration) {
        
      // If any of mandatory or optional attendee has no free time, 
      // return the other group's free time slots. 
      if (mandatoryAttendeesEmptyTimeRanges.isEmpty()) {
        return optionalAttendeesEmptyTimeRanges;
      }
      if (optionalAttendeesEmptyTimeRanges.isEmpty()) {
        return mandatoryAttendeesEmptyTimeRanges; 
      }    

      List<TimeRange> commonTimeRanges = new ArrayList<TimeRange>();
      int i = 0;
      int j = 0;

      while (i < mandatoryAttendeesEmptyTimeRanges.size() && j < optionalAttendeesEmptyTimeRanges.size()) {
        TimeRange mandatorySlot = mandatoryAttendeesEmptyTimeRanges.get(i);
        TimeRange optionalSlot = optionalAttendeesEmptyTimeRanges.get(j);

        // If two time ranges overlap, find the intersection of these two. 
        if (mandatorySlot.overlaps(optionalSlot)) {
          int start = Math.max(mandatorySlot.start(), optionalSlot.start());
          int end = Math.min(mandatorySlot.end(), optionalSlot.end());
          
          if (end - start >= meetingDuration) {
            TimeRange commonSlot = TimeRange.fromStartEnd(
              /* start time = */ start, 
              /* end time = */ end, 
              /* inclusive of end time = */ false);
            commonTimeRanges.add(commonSlot);
          } else {
            commonTimeRanges.add(mandatorySlot); 
          }
        } 

        // Move the pointer pointing at the time range that ends earlier,
        // because the time range that ends later might have intersection with other
        // time ranges in the time ranges taht come after the one that ends earlier.
        if (mandatorySlot.end() >= optionalSlot.end()) {
          j++; 
        } else {
          i++; 
        }   
      }

      return commonTimeRanges; 
  }
}
