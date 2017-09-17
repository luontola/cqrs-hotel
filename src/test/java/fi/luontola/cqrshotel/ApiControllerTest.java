// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.CapacityDto;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    private final UUID roomId = UUID.randomUUID();
    private final LocalDate arrival = LocalDate.now();
    private final LocalDate departure = arrival.plusDays(2);

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
                new SearchForAccommodation(reservationId, arrival, departure),
                ReservationOffer.class);

        ReservationOffer expected = new ReservationOffer();
        expected.reservationId = reservationId;
        expected.arrival = arrival;
        expected.departure = departure;
        expected.totalPrice = pricePerDay.multiply(2);
        assertThat(offer, is(expected));
    }

    @Test
    public void make_reservation() {
        ReservationOffer offer = restTemplate.postForObject("/api/search-for-accommodation",
                new SearchForAccommodation(reservationId, arrival, departure),
                ReservationOffer.class);

        Boolean result = restTemplate.postForObject("/api/make-reservation",
                new MakeReservation(
                        offer.reservationId, offer.arrival, offer.departure,
                        "John Doe", "john@example.com"),
                Boolean.class);

        assertThat(result, is(true));
        test_reservations();
        test_reservationById();
    }

    // XXX: implement the following as dependent tests

    public void test_reservations() {
        // TODO: figure out how to parameterize the list element type
        List<Map<String, Object>> results = restTemplate.getForObject("/api/reservations", List.class);

        String reservationId = this.reservationId.toString();
        assertThat(results.stream()
                .filter(reservation -> reservation.get("reservationId").equals(reservationId))
                .findFirst(), is(notNullValue()));
    }

    public void test_reservationById() {
        ReservationDto result = restTemplate.getForObject("/api/reservations/{id}", ReservationDto.class, reservationId);

        assertThat(result.reservationId, is(reservationId));
    }

    @Test
    public void create_room() {
        Boolean result = restTemplate.postForObject("/api/create-room",
                new CreateRoom(roomId, "123"),
                Boolean.class);

        assertThat(result, is(true));
        test_rooms();
    }

    public void test_rooms() {
        // TODO: figure out how to parameterize the list element type
        List<Map<String, Object>> results = restTemplate.getForObject("/api/rooms", List.class);

        String roomId = this.roomId.toString();
        assertThat(results.stream()
                .filter(room -> room.get("roomId").equals(roomId))
                .findFirst(), is(notNullValue()));
    }

    @Test
    public void test_capacityByDate() {
        LocalDate date = LocalDate.now();

        CapacityDto results = restTemplate.getForObject("/api/capacity/{date}", CapacityDto.class, date);

        assertThat("date", results.date, is(date));
        assertThat("capacity", results.capacity, is(notNullValue()));
        assertThat("reserved", results.reserved, is(notNullValue()));
    }

    @Test
    public void test_capacityByDateRange() {
        LocalDate date = LocalDate.now();

        List<Map<String, Object>> results = restTemplate.getForObject("/api/capacity/{start}/{end}", List.class, date, date);

        assertThat(results, hasSize(1));
    }
}
