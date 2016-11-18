// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitialized;
import fi.luontola.cqrshotel.reservation.events.ReservationMade;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Category(FastTests.class)
public class MakeReservationTest extends AggregateRootTester {

    private static final LocalDate startDate = LocalDate.of(2000, 1, 2);
    private static final LocalDate endDate = LocalDate.of(2001, 3, 4);

    {
        commandHandler = new MakeReservationHandler(new ReservationRepo(eventStore));
    }

    @Test
    public void make_reservation() {
        given(new ReservationInitialized(id));
        when(new MakeReservation(id, startDate, endDate, "John Doe", "john@example.com"));
        then(new ContactInformationUpdated(id, "John Doe", "john@example.com"),
                new ReservationMade(id,
                        ZonedDateTime.of(startDate, Reservation.CHECK_IN_TIME, Reservation.TIMEZONE).toInstant(),
                        ZonedDateTime.of(endDate, Reservation.CHECK_OUT_TIME, Reservation.TIMEZONE).toInstant()));
    }

    // TODO: create line items
    // TODO: check price offer existence
    // TODO: check price offer expiry
}
