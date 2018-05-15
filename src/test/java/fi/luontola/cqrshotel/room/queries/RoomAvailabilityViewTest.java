// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
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

    private final RoomAvailabilityView view = new RoomAvailabilityView(new InMemoryEventStore());

    @Test
    public void lists_all_rooms() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomCreated(roomId2, "102"));

        List<RoomAvailabilityDto> results = view.getAvailabilityForAllRooms(t1, t2);
        assertThat(results, hasSize(2));
    }

    @Test
    public void lists_room_basic_information() {
        view.apply(new RoomCreated(roomId, "101"));

        RoomAvailabilityDto dto = view.getAvailabilityForAllRooms(t1, t2).get(0);
        assertThat("roomId", dto.roomId, is(roomId));
        assertThat("roomNumber", dto.roomNumber, is("101"));
    }

    @Test
    public void when_room_is_not_occupied() {
        view.apply(new RoomCreated(roomId, "101"));

        assertThat(availabilityBetween(t1, t2), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, false))));
    }

    @Test
    public void when_room_is_occupied_exactly_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));

        assertThat(availabilityBetween(t1, t2), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true))));
    }

    @Test
    public void when_room_is_occupied_longer_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t4, occupant));

        assertThat(availabilityBetween(t2, t3), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t4, true))));
    }

    @Test
    public void when_room_is_occupied_shorter_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t2, t3, occupant));

        assertThat(availabilityBetween(t1, t4), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, false),
                new RoomAvailabilityIntervalDto(t2, t3, true),
                new RoomAvailabilityIntervalDto(t3, t4, false))));
    }

    @Test
    public void occupied_intervals_are_listed_in_chronological_order() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant2)); // NOT in chronological order
        view.apply(new RoomOccupied(roomId, t2, t3, occupant3));

        assertThat(availabilityBetween(t1, t4), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true),
                new RoomAvailabilityIntervalDto(t2, t3, true),
                new RoomAvailabilityIntervalDto(t3, t4, true))));
    }

    @Test
    public void excludes_occupied_intervals_outside_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t2, t3, occupant2));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant3));

        assertThat(availabilityBetween(t2, t3), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t2, t3, true))));
    }

    @Test
    public void fills_unoccupied_intervals_between_occupied_intervals() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant2));

        assertThat(availabilityBetween(t1, t4), is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true),
                new RoomAvailabilityIntervalDto(t2, t3, false),
                new RoomAvailabilityIntervalDto(t3, t4, true))));
    }

    private List<RoomAvailabilityIntervalDto> availabilityBetween(Instant start, Instant end) {
        return view.getAvailabilityForAllRooms(start, end).get(0).availability;
    }
}
