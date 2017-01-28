// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.stub;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Category(SlowTests.class)
public class ApiControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    PricingEngine pricingEngine;

    private final Money pricePerDay = Money.of(100, "EUR");
    private final UUID reservationId = UUID.randomUUID();
    private final LocalDate startDate = LocalDate.now();
    private final LocalDate endDate = startDate.plusDays(2);

    @Before
    public void initMocks() {
        stub(pricingEngine.getAccommodationPrice(any(LocalDate.class)))
                .toReturn(Optional.of(pricePerDay));
    }

    @Test
    public void home() {
        String response = restTemplate.getForObject("/api", String.class);

        assertThat(response, containsString("CQRS Hotel"));
    }

    @Test
    public void search_for_accommodation() {
        ReservationOffer offer = restTemplate.postForObject("/api/search-for-accommodation",
                new SearchForAccommodation(reservationId, startDate, endDate),
                ReservationOffer.class);

        ReservationOffer expected = new ReservationOffer();
        expected.reservationId = reservationId;
        expected.startDate = startDate;
        expected.endDate = endDate;
        expected.totalPrice = pricePerDay.multiply(2);
        assertThat(offer, is(expected));
    }

    @Test
    public void make_reservation() {
        ReservationOffer offer = restTemplate.postForObject("/api/search-for-accommodation",
                new SearchForAccommodation(reservationId, startDate, endDate),
                ReservationOffer.class);

        Boolean reservation = restTemplate.postForObject("/api/make-reservation",
                new MakeReservation(
                        offer.reservationId, offer.startDate, offer.endDate,
                        "John Doe", "john@example.com"),
                Boolean.class);

        assertThat(reservation, is(true));
    }
}
