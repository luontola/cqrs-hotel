// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.reservation.queries.SearchForAccommodationQueryHandler;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Category(FastTests.class)
public class SearchForAccommodationQueryHandlerTest {

    private final UUID id = UUID.randomUUID();
    private static final LocalDate date1 = LocalDate.of(2000, 1, 1);
    private static final LocalDate date2 = LocalDate.of(2000, 1, 2);
    private static final LocalDate date3 = LocalDate.of(2000, 1, 3);
    private static final Money price1 = Money.of(11, "EUR");
    private static final Money price2 = Money.of(12, "EUR");
    private static final Money price3 = Money.of(13, "EUR");
    private static final Instant now = Instant.now();
    private static final Instant expires = now.plus(Reservation.PRICE_VALIDITY_DURATION);

    private final EventStore eventStore = new InMemoryEventStore();
    private final SearchForAccommodationQueryHandler queryHandler = new SearchForAccommodationQueryHandler(eventStore, Clock.fixed(now, ZoneId.systemDefault()));

    @Test
    public void calculates_total_price_from_price_offers() {
        given(
                new PriceOffered(id, date1, price1, expires),
                new PriceOffered(id, date2, price2, expires)
        );

        ReservationOffer result = queryHandler.handle(new SearchForAccommodation(id, date1, date3));

        ReservationOffer expected = new ReservationOffer();
        expected.reservationId = id;
        expected.arrival = date1;
        expected.departure = date3;
        expected.totalPrice = price1.add(price2);
        assertThat(result, is(expected));
    }


    @Test
    public void ignores_price_offers_for_other_days() {
        given(
                new PriceOffered(id, date1, price1, expires),
                new PriceOffered(id, date2, price2, expires),
                new PriceOffered(id, date3, price3, expires)
        );

        ReservationOffer result = queryHandler.handle(new SearchForAccommodation(id, date2, date3));

        assertThat("totalPrice", result.totalPrice, is(price2));
    }

    @Test
    public void is_unavailable_if_a_price_offer_is_missing() {
        given(
                new PriceOffered(id, date2, price2, expires)
        );

        ReservationOffer result = queryHandler.handle(new SearchForAccommodation(id, date1, date3));

        assertThat("totalPrice", result.totalPrice, is(nullValue()));
    }

    @Test
    public void is_unavailable_if_a_price_offer_has_expired() {
        given(
                new PriceOffered(id, date1, price1, expires),
                new PriceOffered(id, date2, price2, now.minusSeconds(1))
        );

        ReservationOffer result = queryHandler.handle(new SearchForAccommodation(id, date1, date3));

        assertThat("totalPrice", result.totalPrice, is(nullValue()));
    }

    private void given(Event... events) {
        eventStore.saveEvents(id,
                Arrays.stream(events)
                        .map(Envelope::newMessage)
                        .collect(Collectors.toList()),
                EventStore.BEGINNING);
    }
}
