// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ReservationsViewTest {

    private static final UUID reservationId = UUID.randomUUID();
    private static final UUID reservationId2 = UUID.randomUUID();
    private static final ZonedDateTime checkInTime = ZonedDateTime.of(2000, 1, 3, 14, 30, 0, 0, ZoneId.of("Europe/Helsinki"));
    private static final ZonedDateTime checkOutTime = ZonedDateTime.of(2000, 2, 4, 10, 30, 0, 0, ZoneId.of("Europe/Helsinki"));

    @Test
    public void fills_in_all_fields() {
        ReservationsView view = new ReservationsView(new InMemoryEventStore());
        view.apply(new ReservationInitiated(reservationId, checkInTime, checkOutTime));
        view.apply(new ContactInformationUpdated(reservationId, "name", "email"));

        ReservationDto expected = new ReservationDto();
        expected.reservationId = reservationId;
        expected.checkInTime = "3.1.2000 14:30";
        expected.checkOutTime = "4.2.2000 10:30";
        expected.name = "name";
        expected.email = "email";
        expected.status = "initiated";
        List<ReservationDto> results = view.findAll();
        assertThat(results, hasSize(1));
        assertThat(results.get(0), is(expected));
    }

    @Test
    public void lists_all_reservations() {
        ReservationsView view = new ReservationsView(new InMemoryEventStore());
        view.apply(new ReservationInitiated(reservationId, checkInTime, checkOutTime));
        view.apply(new ReservationInitiated(reservationId2, checkInTime, checkOutTime));

        List<ReservationDto> results = view.findAll();
        assertThat(results, hasSize(2));
    }
}
