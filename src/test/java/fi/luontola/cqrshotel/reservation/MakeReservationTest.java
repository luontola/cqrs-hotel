// Copyright © 2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.events.*;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.*;

@Category(FastTests.class)
public class MakeReservationTest extends AggregateRootTester {

    private static final LocalDate date1 = LocalDate.of(2000, 1, 11);
    private static final LocalDate date2 = LocalDate.of(2000, 1, 12);
    private static final LocalDate date3 = LocalDate.of(2000, 1, 13);
    private static final LocalDate date4 = LocalDate.of(2000, 1, 14);
    private static final Money price1 = Money.of(101, "EUR");
    private static final Money price2 = Money.of(102, "EUR");
    private static final Money price3 = Money.of(103, "EUR");
    private static final Instant now = Instant.now();
    private static final Instant expires = now.plus(Duration.ofHours(1));

    {
        commandHandler = new MakeReservationHandler(new ReservationRepo(eventStore), Clock.fixed(now, ZoneId.systemDefault()));
    }

    @Test
    public void make_reservation() {
        given(new ReservationInitialized(id),
                new PriceOffered(id, date1, price1, expires));
        when(new MakeReservation(id, date1, date2, "John Doe", "john@example.com"));
        then(new ContactInformationUpdated(id, "John Doe", "john@example.com"),
                new ReservationMade(id,
                        ZonedDateTime.of(date1, Hotel.CHECK_IN_TIME, Hotel.TIMEZONE).toInstant(),
                        ZonedDateTime.of(date2, Hotel.CHECK_OUT_TIME, Hotel.TIMEZONE).toInstant()),
                new LineItemCreated(id, 1, date1, price1));
    }

    @Test
    public void produces_line_items_for_every_date_in_range() {
        given(new ReservationInitialized(id),
                new PriceOffered(id, date1, price1, expires),
                new PriceOffered(id, date2, price2, expires),
                new PriceOffered(id, date3, price3, expires));
        when(new MakeReservation(id, date1, date4, "John Doe", "john@example.com"));
        then(e -> e instanceof LineItemCreated,
                new LineItemCreated(id, 1, date1, price1),
                new LineItemCreated(id, 2, date2, price2),
                new LineItemCreated(id, 3, date3, price3));
    }

    // TODO: check price offer existence
    // TODO: check price offer expiry
}
