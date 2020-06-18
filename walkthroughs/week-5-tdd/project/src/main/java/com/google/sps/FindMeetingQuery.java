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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FindMeetingQuery {

  /**
   * Finds a list of potential meeting times based on the meeting request and a list of existing
   * events and their attendees. Picks time slots if one or more time slots exists so that both
   * mandatory and optional attendees can attend. Otherwise, returns the time slots that fit just
   * the mandatory attendees.
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    Collection<String> mandatoryAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long meetingDurationMinutes = request.getDuration();

    List<TimeRange> mandatoryAttendeesBusyTimeRanges =
        getAllBusyTimeRanges(events, mandatoryAttendees);
    List<TimeRange> optionalAttendeesBusyTimeRanges =
        getAllBusyTimeRanges(events, optionalAttendees);

    sortTimeRangesByStart(mandatoryAttendeesBusyTimeRanges);
    sortTimeRangesByStart(optionalAttendeesBusyTimeRanges);

    List<TimeRange> mandatoryAttendeesFreeTimeRanges =
        getFreeTimeRanges(mandatoryAttendeesBusyTimeRanges, meetingDurationMinutes);
    List<TimeRange> optionalAttendeesFreeTimeRanges =
        getFreeTimeRanges(optionalAttendeesBusyTimeRanges, meetingDurationMinutes);

    if (mandatoryAttendees.isEmpty()) {
      return optionalAttendeesFreeTimeRanges;
    } else if (optionalAttendees.isEmpty()) {
      return mandatoryAttendeesFreeTimeRanges;
    } else {
      return findCommonFreeTimeRanges(
          mandatoryAttendeesFreeTimeRanges,
          optionalAttendeesFreeTimeRanges,
          meetingDurationMinutes);
    }
  }

  /**
   * Constructs all the time ranges that any of the mandatory attendees is busy. These time ranges
   * cannot be part of the potential meeting time.
   */
  private List<TimeRange> getAllBusyTimeRanges(
      Collection<Event> events, Collection<String> mandatoryAttendees) {
    List<TimeRange> busyTimes = new ArrayList<TimeRange>();

    for (Event event : events) {
      Collection<String> eventAttendees = event.getAttendees();

      for (String attendee : eventAttendees) {
        // If the current event's attendee list contains a mandatory attendee,
        // adds this event's time range to the set of all busy times,
        // after checking for overlapping time ranges.
        if (mandatoryAttendees.contains(attendee)) {
          TimeRange currEventTimeRange = event.getWhen();

          busyTimes = getUpdatedBusyTimeRanges(currEventTimeRange, busyTimes);

          // If any one of the mandatory attendees is in this event's guest list,
          // this event's time range needs to be considered busy.
          // Breaks out of the for-each loop because there is no need to look
          // at the rest of the event's guest list.
          break;
        }
      }
    }
    return busyTimes;
  }

  /**
   * Compares a given event's time slot with all the existing busy time slots. Merges the current
   * event time slot with a busy time slot if they overlap. Adds the current event's time slot to
   * the list of busy slots in the end.
   *
   * @param current event's time range; a collection of busy time slots
   * @return an updated list of busy times after overlaps are handled
   */
  private List<TimeRange> getUpdatedBusyTimeRanges(
      TimeRange currEventTimeRange, Collection<TimeRange> originalBusyTimes) {
    boolean isOverlapped = false;

    List<TimeRange> updatedBusyTimes = new ArrayList<TimeRange>();

    for (TimeRange busyTimeRange : originalBusyTimes) {
      if (currEventTimeRange.overlaps(busyTimeRange)) {
        TimeRange combinedTimeRange = getCombinedTimeRange(currEventTimeRange, busyTimeRange);
        updatedBusyTimes.add(combinedTimeRange);
        isOverlapped = true;
        currEventTimeRange = combinedTimeRange;
      } else {
        updatedBusyTimes.add(busyTimeRange);
      }
    }

    // If the current event doesn't overlap with any existing busy time range,
    // add the current event's time range to the set of all busy time slots.
    if (!isOverlapped) {
      updatedBusyTimes.add(currEventTimeRange);
    }

    return updatedBusyTimes;
  }

  /** Creates a new overall combined time range for two time ranges that overlap. */
  private static TimeRange getCombinedTimeRange(TimeRange a, TimeRange b) {
    int aStart = a.start();
    int aEnd = a.end();
    int bStart = b.start();
    int bEnd = b.end();

    TimeRange combinedTimeRange =
        TimeRange.fromStartEnd(
            /* start= */ Math.min(aStart, bStart),
            /* end= */ Math.max(aEnd, bEnd),
            /* inclusive= */ false);

    return combinedTimeRange;
  }

  /** Sorts a list of time ranges by their start time. */
  private void sortTimeRangesByStart(List<TimeRange> allTimeRanges) {
    Collections.sort(allTimeRanges, TimeRange.ORDER_BY_START);
  }

  /**
   * Finds all free time ranges based on a list of busy time ranges and the length of the requested
   * meeting.
   */
  private List<TimeRange> getFreeTimeRanges(
      List<TimeRange> allBusyTimeRanges, long meetingDurationMinutes) {
    List<TimeRange> freeTimeRanges = new ArrayList<TimeRange>();

    if (allBusyTimeRanges.isEmpty() && meetingDurationMinutes <= TimeRange.END_OF_DAY) {
      freeTimeRanges.add(
          TimeRange.fromStartEnd(
              /* start= */ 0, /* end= */ TimeRange.END_OF_DAY, /* inclusive= */ true));
      return freeTimeRanges;
    }

    // A for loop is necessary because each consecutive pair
    // of time ranges needs to be looked at at each iteration.
    for (int i = 0; i < allBusyTimeRanges.size(); i++) {
      if (i == 0) {
        int earliestStart = allBusyTimeRanges.get(i).start();
        if (earliestStart >= meetingDurationMinutes) {
          freeTimeRanges.add(
              TimeRange.fromStartEnd(
                  /* start= */ TimeRange.START_OF_DAY,
                  /* end= */ earliestStart,
                  /* inclusive= */ false));
        }
      }

      if (i == allBusyTimeRanges.size() - 1) {
        int latestEnd = allBusyTimeRanges.get(i).end();
        if ((TimeRange.END_OF_DAY - latestEnd) >= meetingDurationMinutes) {
          // According to TimeRange class's implementation of the time ranges,
          // TimeRange.END_OF_DAY variable should have "true" for the
          // "inclusive" parameter.
          freeTimeRanges.add(
              TimeRange.fromStartEnd(
                  /* start= */ latestEnd, /* end= */ TimeRange.END_OF_DAY, /* inclusive= */ true));
        }
      } else {
        int currEnd = allBusyTimeRanges.get(i).end();
        int nextStart = allBusyTimeRanges.get(i + 1).start();

        if (nextStart - currEnd >= meetingDurationMinutes) {
          freeTimeRanges.add(
              TimeRange.fromStartEnd(
                  /* start= */ currEnd, /* end= */ nextStart, /* inclusive= */ false));
        }
      }
    }
    return freeTimeRanges;
  }

  /** Finds the common time range between time ranges of two collections. */
  private List<TimeRange> findCommonFreeTimeRanges(
      List<TimeRange> mandatoryAttendeesFreeTimeRanges,
      List<TimeRange> optionalAttendeesFreeTimeRanges,
      long meetingDurationMinutes) {

    // If any of mandatory or optional attendee has no free time,
    // return the other group's free time slots.
    if (mandatoryAttendeesFreeTimeRanges.isEmpty()) {
      return optionalAttendeesFreeTimeRanges;
    }
    if (optionalAttendeesFreeTimeRanges.isEmpty()) {
      return mandatoryAttendeesFreeTimeRanges;
    }

    List<TimeRange> commonTimeRanges = new ArrayList<TimeRange>();
    // i indexes into mandatoryAttendeesFreeTimeRanges
    // j indexes into optionalAttendeesFreeTimeRanges
    int i = 0;
    int j = 0;

    while (i < mandatoryAttendeesFreeTimeRanges.size()
        && j < optionalAttendeesFreeTimeRanges.size()) {
      TimeRange mandatoryTimeRange = mandatoryAttendeesFreeTimeRanges.get(i);
      TimeRange optionalTimeRange = optionalAttendeesFreeTimeRanges.get(j);

      // If two time ranges overlap, find the intersection of these two.
      if (mandatoryTimeRange.overlaps(optionalTimeRange)) {
        int start = Math.max(mandatoryTimeRange.start(), optionalTimeRange.start());
        int end = Math.min(mandatoryTimeRange.end(), optionalTimeRange.end());

        if (end - start >= meetingDurationMinutes) {
          TimeRange commonTimeRange =
              TimeRange.fromStartEnd(/* start= */ start, /* end= */ end, /* inclusive= */ false);
          commonTimeRanges.add(commonTimeRange);
        } else {
          commonTimeRanges.add(mandatoryTimeRange);
        }
      }

      // Move the pointer pointing at the time range that ends earlier,
      // because the time range that ends later might have intersection with other
      // time ranges in the time ranges taht come after the one that ends earlier.
      if (mandatoryTimeRange.end() >= optionalTimeRange.end()) {
        j++;
      } else {
        i++;
      }
    }

    return commonTimeRanges;
  }
}
