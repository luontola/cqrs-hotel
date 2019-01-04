// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.reservation.events.RoomAssigned;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ReservationsViewTest {

    private static final UUID reservationId = UUID.randomUUID();
    private static final UUID reservationId2 = UUID.randomUUID();
    private static final LocalDate arrival = LocalDate.of(2000, 1, 3);
    private static final LocalDate departure = LocalDate.of(2000, 2, 4);
    private static final ZonedDateTime checkInTime = ZonedDateTime.of(arrival, LocalTime.of(14, 30), ZoneId.of("Europe/Helsinki"));
    private static final ZonedDateTime checkOutTime = ZonedDateTime.of(departure, LocalTime.of(10, 30), ZoneId.of("Europe/Helsinki"));
    private static final UUID roomId = UUID.randomUUID();

    private final ReservationsView view = new ReservationsView();

    @Test
    public void fills_in_all_fields() {
        view.apply(new ReservationCreated(reservationId, arrival, departure, checkInTime, checkOutTime));
        view.apply(new ContactInformationUpdated(reservationId, "name", "email"));
        view.apply(new RoomAssigned(reservationId, roomId, "123"));

        var expected = new ReservationDto();
        expected.reservationId = reservationId;
        expected.arrival = arrival;
        expected.departure = departure;
        expected.checkInTime = "3.1.2000 14:30";
        expected.checkOutTime = "4.2.2000 10:30";
        expected.name = "name";
        expected.email = "email";
        expected.status = "initiated";
        expected.roomId = roomId;
        expected.roomNumber = "123";

        assertThat(view.findAll(), is(Collections.singletonList(expected)));
    }

    @Test
    public void lists_all_reservations() {
        view.apply(new ReservationCreated(reservationId, arrival, departure, checkInTime, checkOutTime));
        view.apply(new ReservationCreated(reservationId2, arrival, departure, checkInTime, checkOutTime));

        var results = view.findAll();
        assertThat(results, hasSize(2));
    }

    @Test
    public void finds_reservations_by_id() {
        view.apply(new ReservationCreated(reservationId, arrival, departure, checkInTime, checkOutTime));
        view.apply(new ReservationCreated(reservationId2, arrival, departure, checkInTime, checkOutTime));

        var result = view.findById(reservationId);
        assertThat(result.reservationId, is(reservationId));
    }
}
