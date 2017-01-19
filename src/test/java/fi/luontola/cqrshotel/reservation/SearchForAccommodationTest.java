// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.pricing.InMemoryPricingEngine;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationHandler;
import fi.luontola.cqrshotel.reservation.events.CustomerDiscovered;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import fi.luontola.cqrshotel.reservation.events.SearchedForAccommodation;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Category(FastTests.class)
public class SearchForAccommodationTest extends AggregateRootTester {

    private static final LocalDate date1 = LocalDate.of(2000, 1, 2);
    private static final LocalDate date2 = LocalDate.of(2000, 1, 3);
    private static final LocalDate date3 = LocalDate.of(2000, 1, 4);
    private static final Money price1 = Money.of(11, "EUR");
    private static final Money price2 = Money.of(12, "EUR");
    private static final Instant now = Instant.now();
    private static final Instant expires = now.plus(Reservation.PRICE_VALIDITY_DURATION);

    {
        PricingEngine pricing = new InMemoryPricingEngine()
                .setPrice(date1, price1)
                .setPrice(date2, price2);
        commandHandler = new SearchForAccommodationHandler(new ReservationRepo(eventStore), pricing, Clock.fixed(now, ZoneId.systemDefault()));
    }

    @Test
    public void first_search_initializes_the_reservation_and_records_the_search_and_offered_prices() {
        when(new SearchForAccommodation(id, date1, date3));
        then(new CustomerDiscovered(id),
                new SearchedForAccommodation(id, date1, date3),
                new PriceOffered(id, date1, price1, expires),
                new PriceOffered(id, date2, price2, expires));
    }

    @Test
    public void subsequent_search_records_the_search_and_only_new_offered_prices() {
        given(new CustomerDiscovered(id),
                new SearchedForAccommodation(id, date1, date2),
                new PriceOffered(id, date1, price1, expires));
        when(new SearchForAccommodation(id, date1, date3));
        then(new SearchedForAccommodation(id, date1, date3),
                new PriceOffered(id, date2, price2, expires));
    }

    @Test
    public void if_price_not_available_then_does_not_record_a_price_offer() {
        given(new CustomerDiscovered(id));
        when(new SearchForAccommodation(id, date3, date3.plusDays(1)));
        then(new SearchedForAccommodation(id, date3, date3.plusDays(1)));
    }

    @Test
    public void if_old_price_offer_has_expired_then_produces_a_new_price_offer() {
        given(new CustomerDiscovered(id),
                new PriceOffered(id, date1, Money.of(9, "EUR"), now.minusSeconds(1)));
        when(new SearchForAccommodation(id, date1, date2));
        then(new SearchedForAccommodation(id, date1, date2),
                new PriceOffered(id, date1, price1, expires));
    }
}
