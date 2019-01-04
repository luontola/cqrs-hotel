// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.LineItemCreated;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import org.javamoney.moneta.Money;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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
    private static final Instant expiresInFuture = now.plus(Duration.ofHours(1));

    {
        commandHandler = new MakeReservationHandler(new ReservationRepo(eventStore), Clock.fixed(now, ZoneId.systemDefault()));
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void make_reservation_for_one_night() {
        given(new PriceOffered(id, date1, price1, expiresInFuture));

        when(new MakeReservation(id, date1, date2, "John Doe", "john@example.com"));

        then(new ContactInformationUpdated(id, "John Doe", "john@example.com"),
                new ReservationCreated(id, date1, date2, Hotel.checkInTime(date1), Hotel.checkOutTime(date2)),
                new LineItemCreated(id, 1, date1, price1));
    }

    @Test
    public void cannot_make_reservation_twice() {
        given(new ReservationCreated(id, date1, date2, Hotel.checkInTime(date1), Hotel.checkOutTime(date2)));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("unexpected state: RESERVED");
        when(new MakeReservation(id, date1, date2, "John Doe", "john@example.com"));
    }

    @Test
    public void produces_line_items_for_every_date_in_range() {
        given(new PriceOffered(id, date1, price1, expiresInFuture),
                new PriceOffered(id, date2, price2, expiresInFuture),
                new PriceOffered(id, date3, price3, expiresInFuture));

        when(new MakeReservation(id, date1, date4, "John Doe", "john@example.com"));

        then(event -> event instanceof LineItemCreated,
                new LineItemCreated(id, 1, date1, price1),
                new LineItemCreated(id, 2, date2, price2),
                new LineItemCreated(id, 3, date3, price3));
    }

    @Test
    public void rejects_if_a_price_offer_is_missing() {
        given(new PriceOffered(id, date1, price1, expiresInFuture),
                new PriceOffered(id, date2, price2, expiresInFuture));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("no price offer for date 2000-01-13");
        when(new MakeReservation(id, date1, date4, "John Doe", "john@example.com"));
    }

    @Test
    public void rejects_if_a_price_offer_has_expired() {
        given(new PriceOffered(id, date1, price1, expiresInFuture),
                new PriceOffered(id, date2, price2, expiresInFuture),
                new PriceOffered(id, date3, price3, now));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("price offer for date 2000-01-13 has expired");
        when(new MakeReservation(id, date1, date4, "John Doe", "john@example.com"));
    }

    @Test
    public void uses_the_new_price_if_an_expired_price_offer_has_been_replaced() {
        var newPrice3 = price3.add(Money.of(10, "EUR"));
        given(new PriceOffered(id, date1, price1, expiresInFuture),
                new PriceOffered(id, date2, price2, expiresInFuture),
                new PriceOffered(id, date3, price3, now),
                new PriceOffered(id, date3, newPrice3, expiresInFuture));

        when(new MakeReservation(id, date1, date4, "John Doe", "john@example.com"));

        then(event -> event instanceof LineItemCreated,
                new LineItemCreated(id, 1, date1, price1),
                new LineItemCreated(id, 2, date2, price2),
                new LineItemCreated(id, 3, date3, newPrice3));
    }
}
