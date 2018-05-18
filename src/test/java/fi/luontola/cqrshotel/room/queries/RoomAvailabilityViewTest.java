// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class RoomAvailabilityViewTest {

    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();
    private static final UUID occupant = UUID.randomUUID();
    private static final UUID occupant2 = UUID.randomUUID();
    private static final UUID occupant3 = UUID.randomUUID();
    private static final Instant t1 = Instant.parse("2018-01-01T12:00:00.000Z");
    private static final Instant t2 = Instant.parse("2018-01-02T12:00:00.000Z");
    private static final Instant t3 = Instant.parse("2018-01-03T12:00:00.000Z");
    private static final Instant t4 = Instant.parse("2018-01-04T12:00:00.000Z");

    private final RoomAvailabilityView view = new RoomAvailabilityView();

    @Test
    public void lists_all_rooms() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomCreated(roomId2, "102"));

        List<RoomAvailabilityDto> rooms = view.getAvailabilityForAllRooms(t1, t2);
        assertThat(rooms, hasSize(2));
    }

    @Test
    public void lists_room_basic_information() {
        view.apply(new RoomCreated(roomId, "101"));

        RoomAvailabilityDto availability = availabilityBetween(t1, t2);
        assertThat("roomId", availability.roomId, is(roomId));
        assertThat("roomNumber", availability.roomNumber, is("101"));
    }

    @Test
    public void when_room_is_not_occupied() {
        view.apply(new RoomCreated(roomId, "101"));

        RoomAvailabilityDto availability = availabilityBetween(t1, t2);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, false))));
        assertThat("available?", availability.available, is(true));
    }

    @Test
    public void when_room_is_occupied_exactly_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));

        RoomAvailabilityDto availability = availabilityBetween(t1, t2);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void when_room_is_occupied_longer_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t4, occupant));

        RoomAvailabilityDto availability = availabilityBetween(t2, t3);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t4, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void when_room_is_occupied_shorter_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t2, t3, occupant));

        RoomAvailabilityDto availability = availabilityBetween(t1, t4);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, false),
                new RoomAvailabilityIntervalDto(t2, t3, true),
                new RoomAvailabilityIntervalDto(t3, t4, false))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void occupied_intervals_are_listed_in_chronological_order() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant2)); // NOT in chronological order
        view.apply(new RoomOccupied(roomId, t2, t3, occupant3));

        RoomAvailabilityDto availability = availabilityBetween(t1, t4);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true),
                new RoomAvailabilityIntervalDto(t2, t3, true),
                new RoomAvailabilityIntervalDto(t3, t4, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void excludes_occupied_intervals_outside_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t2, t3, occupant2));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant3));

        RoomAvailabilityDto availability = availabilityBetween(t2, t3);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t2, t3, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void fills_unoccupied_intervals_between_occupied_intervals() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant2));

        RoomAvailabilityDto availability = availabilityBetween(t1, t4);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true),
                new RoomAvailabilityIntervalDto(t2, t3, false),
                new RoomAvailabilityIntervalDto(t3, t4, true))));
        assertThat("available?", availability.available, is(false));
    }

    private RoomAvailabilityDto availabilityBetween(Instant start, Instant end) {
        return view.getAvailabilityForAllRooms(start, end).get(0);
    }
}
