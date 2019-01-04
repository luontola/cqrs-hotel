// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.capacity.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Category(FastTests.class)
public class CapacityViewTest {

    private static final LocalDate date1 = LocalDate.of(2000, 1, 1);
    private static final LocalDate date2 = LocalDate.of(2000, 1, 2);
    private static final LocalDate date3 = LocalDate.of(2000, 1, 3);
    private static final LocalDate date4 = LocalDate.of(2000, 1, 4);
    private static final LocalDate date5 = LocalDate.of(2000, 1, 5);

    private final CapacityView view = new CapacityView();

    @Test
    public void default_values() {
        CapacityDto capacity = view.getCapacityByDate(date1);

        assertThat("date", capacity.date, is(date1));
        assertThat("capacity", capacity.capacity, is(0));
        assertThat("reserved", capacity.reserved, is(0));
    }

    @Test
    public void capacity_equals_the_number_of_rooms() {
        view.apply(new RoomCreated(UUID.randomUUID(), "101"));
        view.apply(new RoomCreated(UUID.randomUUID(), "102"));

        CapacityDto capacity = view.getCapacityByDate(date1);

        assertThat("capacity", capacity.capacity, is(2));
        assertThat("reserved", capacity.reserved, is(0)); // unaffected
    }

    @Test
    public void reserved_equals_the_number_of_reservations() {
        view.apply(new ReservationCreated(UUID.randomUUID(), date1, date2, null, null));
        view.apply(new ReservationCreated(UUID.randomUUID(), date1, date2, null, null));

        CapacityDto capacity = view.getCapacityByDate(date1);

        assertThat("capacity", capacity.capacity, is(0)); // unaffected
        assertThat("reserved", capacity.reserved, is(2));
    }

    @Test
    public void reservations_affect_only_the_range_from_arrival_inclusive_to_departure_exclusive() {
        view.apply(new ReservationCreated(UUID.randomUUID(), date2, date4, null, null));

        assertThat("date1 (before)",
                view.getCapacityByDate(date1).reserved, is(0));
        assertThat("date2 (arrival)",
                view.getCapacityByDate(date2).reserved, is(1));
        assertThat("date3 (during)",
                view.getCapacityByDate(date3).reserved, is(1));
        assertThat("date4 (departure)",
                view.getCapacityByDate(date4).reserved, is(0));
        assertThat("date5 (after)",
                view.getCapacityByDate(date5).reserved, is(0));
    }

    @Test
    public void lists_capacity_by_date_range() {
        List<CapacityDto> results = view.getCapacityByDateRange(date1, date3);

        assertThat(results, hasSize(3));
        CapacityDto capacity = results.get(0);
        assertThat(capacity.date, is(date1));
        assertThat(capacity.capacity, is(notNullValue()));
        assertThat(capacity.reserved, is(notNullValue()));
    }
}
