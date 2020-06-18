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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class FindMeetingQueryTest {
  private static final Collection<Event> NO_EVENTS = Collections.emptySet();
  private static final Collection<String> NO_ATTENDEES = Collections.emptySet();

  // Some people that we can use in our tests.
  private static final String PERSON_A = "Person A";
  private static final String PERSON_B = "Person B";
  private static final String PERSON_C = "Person C";

  // All dates are the first day of the year 2020.
  private static final int TIME_0000AM = TimeRange.getTimeInMinutes(0, 0);
  private static final int TIME_0800AM = TimeRange.getTimeInMinutes(8, 0);
  private static final int TIME_0830AM = TimeRange.getTimeInMinutes(8, 30);
  private static final int TIME_0900AM = TimeRange.getTimeInMinutes(9, 0);
  private static final int TIME_0930AM = TimeRange.getTimeInMinutes(9, 30);
  private static final int TIME_1000AM = TimeRange.getTimeInMinutes(10, 0);
  private static final int TIME_1100AM = TimeRange.getTimeInMinutes(11, 00);
  private static final int TIME_1200PM = TimeRange.getTimeInMinutes(12, 00);
  private static final int TIME_0500PM = TimeRange.getTimeInMinutes(17, 00);

  private static final int DURATION_15_MINUTES = 15;
  private static final int DURATION_30_MINUTES = 30;
  private static final int DURATION_60_MINUTES = 60;
  private static final int DURATION_90_MINUTES = 90;
  private static final int DURATION_1_HOUR = 60;
  private static final int DURATION_2_HOUR = 120;
  private static final int DURATION_24_HOUR = 24 * 60 - 1;

  private FindMeetingQuery query;

  @Before
  public void setUp() {
    query = new FindMeetingQuery();
  }

  @Test
  public void optionsForNoAttendees() {
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noOptionsForTooLongOfARequest() {
    // The duration should be longer than a day. This means there should be no options.
    int duration = TimeRange.WHOLE_DAY.duration() + 1;
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), duration);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void eventSplitsRestriction() {
    // The event should split the day into two options (before and after the event).
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0900AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsidered() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0800AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0800AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0830AM, 
                /* end= */ TIME_0900AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0930AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overlappingEvents() {
    // Have an event for each person, but have their events overlap. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0830AM, /* duration= */ DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_60_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_1000AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void nestedEvents() {
    // Have an event for each person, but have one person's event fully contain another's event. We
    // should see two options.
    //
    // Events  :       |----A----|
    //                   |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0830AM, /* duration= */ DURATION_90_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_1000AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void doubleBookedPeople() {
    // Have one person, but have them registered to attend two events at the same time.
    //
    // Events  :       |----A----|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0830AM, /* duration= */ DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0930AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoom() {
    // Have one person, but make it so that there is just enough room at one point in the day to
    // have the meeting.
    //
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0900AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(
            /* start= */ TIME_0830AM, 
            /* duration= */ DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void ignoresPeopleNotAttending() {
    // Add an event, but make the only attendee someone different from the person looking to book
    // a meeting. This event should not affect the booking.
    Collection<Event> events = Arrays.asList(
        new Event("Event 1",
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES), 
            Arrays.asList(PERSON_A)));
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noConflicts() {
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void notEnoughRoom() {
    // Have one person, but make it so that there is not enough room at any point in the day to
    // have the meeting.
    //
    // Events  : |--A-----| |-----A----|
    // Day     : |---------------------|
    // Options :

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0900AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }
  
  // Newly added tests below
 
  @Test
  public void everyAttendeeIsConsideredWithOptionalFullDay() {
    // Have each person have different events. 
    // Optional guest C has an all day event. 
    // We should see two options because each person has
    // split the restricted times. Guest C is not invited. 
    //
    // Optional: |-------------C---------------|
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0800AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0000AM, /* duration= */ DURATION_24_HOUR),
            Arrays.asList(PERSON_C)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
      
    request.addOptionalAttendee(PERSON_C); 
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end = */ TIME_0800AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0830AM, 
                /* end = */ TIME_0900AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0930AM, 
                /* end = */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));
 
    Assert.assertEquals(expected, actual);
  }
   
  @Test
  public void everyAttendeeIsConsideredWithOptionalShortEvent() {
    // Have each person have different events. 
    // Optional guest C has an event for 8:30 - 9:00. 
    //
    // Optional:             |--C--|
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0800AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0900AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0830AM, /* duration= */ DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
      
    request.addOptionalAttendee(PERSON_C); 
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0800AM, 
                /* inclusive= */ false),
            TimeRange.fromStartEnd(
                /* start= */ TIME_0930AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true));
 
    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void justEnoughRoomWithOptional() {
    // Optional:        |-B|
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|
    // Optional guest B has an event between 8:30 - 8:45, 
    // but this guest should be ignored, since inviting Guest B will result in
    // a time range shorter than requested time. 
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0830AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0900AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_A)),
        new Event("Event 3", 
            TimeRange.fromStartDuration(
                /* start= */ TIME_0830AM, /* duration= */ DURATION_15_MINUTES),
            Arrays.asList(PERSON_B)));
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B); 
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(
            /* start= */ TIME_0830AM, /* duration= */ DURATION_30_MINUTES));
 
    Assert.assertEquals(expected, actual);
  }
   
  @Test 
  public void noMandatoryAttendee() {
    // Two optional attendees with some gaps in their schedule. 
    // Optional: |--A--|  |--B-| |--A--|
    // Day     : |---------------------|
    // Options :       |--|    |-|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0800AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0900AM, 
                /* end= */ TIME_1000AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_B)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_1100AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_A)));
 
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(
        TimeRange.fromStartEnd(
            /* start= */ TIME_0800AM, 
            /* end= */ TIME_0900AM, 
            /* inclusive= */ false),
        TimeRange.fromStartEnd(
            /* start= */ TIME_1000AM, 
            /* end= */ TIME_1100AM, 
            /* inclusive= */ false));
 
    Assert.assertEquals(expected, actual);
  }
  
  @Test   
  public void optionalAttendeeNoTimeslot() {
    // Two optional attendees with no gap in schedule.
    // There is no suitable meeting time. 
    // Optional: |--A--||-------B------|
    // Day     : |---------------------|
    // Options : None.  
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0800AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0800AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_B)));
 
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();
 
    Assert.assertEquals(expected, actual);
  } 

  @Test
  public void moreMandatoryThanOptionalSlots() {
    // Events:     |-A-|    |--B-|   
    // Optional: |--C--|    |---C------|
    // Day     : |---------------------|
    // Options :       |----|   
    // Only this time block is good for all mandatory and 
    // optional attendees.  

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_0830AM, 
                /* end= */ TIME_0900AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_1100AM, 
                /* end= */ TIME_1200PM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", 
            TimeRange.fromStartEnd(
                /* start= */ TimeRange.START_OF_DAY, 
                /* end= */ TIME_0900AM, 
                /* inclusive= */ false),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", 
            TimeRange.fromStartEnd(
                /* start= */ TIME_1100AM, 
                /* end= */ TimeRange.END_OF_DAY, 
                /* inclusive= */ true),
            Arrays.asList(PERSON_C)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = 
        Arrays.asList(TimeRange.fromStartEnd(
            /* start= */ TIME_0900AM, /* end= */ TIME_1100AM, /* inclusive= */ false));
 
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void meetingIsExactlyTwentyFourHours() {
    // The requested meeting is exactly 24 hours. 
    // No existing events. The entire day should be 
    // returned as possible meeting time. 

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_24_HOUR);
    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
    Assert.assertEquals(expected, actual);
  }
  
}
