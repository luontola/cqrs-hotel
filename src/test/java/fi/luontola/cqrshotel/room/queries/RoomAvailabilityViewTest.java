// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

@Tag("fast")
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
    private final GetAvailabilityByDateRangeHandler getAvailabilityByDateRange = new GetAvailabilityByDateRangeHandler(view);
    private final GetAvailabilityByTimeRangeHandler getAvailabilityByTimeRange = new GetAvailabilityByTimeRangeHandler(view);

    @Test
    public void lists_all_rooms() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomCreated(roomId2, "102"));

        var rooms = getAvailabilityByTimeRange.handle(new GetAvailabilityByTimeRange(t1, t2));
        assertThat(rooms, is(arrayWithSize(2)));
    }

    @Test
    public void lists_room_basic_information() {
        view.apply(new RoomCreated(roomId, "101"));

        var availability = availabilityBetween(t1, t2);
        assertThat("roomId", availability.roomId, is(roomId));
        assertThat("roomNumber", availability.roomNumber, is("101"));
    }

    @Test
    public void when_room_is_not_occupied() {
        view.apply(new RoomCreated(roomId, "101"));

        var availability = availabilityBetween(t1, t2);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, false))));
        assertThat("available?", availability.available, is(true));
    }

    @Test
    public void when_room_is_occupied_exactly_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));

        var availability = availabilityBetween(t1, t2);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void when_room_is_occupied_longer_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t4, occupant));

        var availability = availabilityBetween(t2, t3);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t4, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void when_room_is_occupied_shorter_than_the_queried_interval() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t2, t3, occupant));

        var availability = availabilityBetween(t1, t4);
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

        var availability = availabilityBetween(t1, t4);
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

        var availability = availabilityBetween(t2, t3);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t2, t3, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void fills_unoccupied_intervals_between_occupied_intervals() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomOccupied(roomId, t1, t2, occupant));
        view.apply(new RoomOccupied(roomId, t3, t4, occupant2));

        var availability = availabilityBetween(t1, t4);
        assertThat(availability.details, is(Arrays.asList(
                new RoomAvailabilityIntervalDto(t1, t2, true),
                new RoomAvailabilityIntervalDto(t2, t3, false),
                new RoomAvailabilityIntervalDto(t3, t4, true))));
        assertThat("available?", availability.available, is(false));
    }

    @Test
    public void queries_from_start_of_day_to_end_of_day_in_hotel_timezone() {
        view.apply(new RoomCreated(UUID.randomUUID(), "101"));

        var start = LocalDate.parse("2000-01-01");
        var end = LocalDate.parse("2000-01-05");
        var result = getAvailabilityByDateRange.handle(new GetAvailabilityByDateRange(start, end));

        // XXX: assumes Hotel.TIMEZONE is Europe/Helsinki but it's not apparent in this test (the timezone is not configurable)
        var interval = result[0].details.get(0);
        assertThat("start", interval.start, is(Instant.parse("1999-12-31T22:00:00Z")));
        assertThat("end", interval.end, is(Instant.parse("2000-01-05T22:00:00Z")));
    }

    private RoomAvailabilityDto availabilityBetween(Instant start, Instant end) {
        return getAvailabilityByTimeRange.handle(new GetAvailabilityByTimeRange(start, end))[0];
    }
}
