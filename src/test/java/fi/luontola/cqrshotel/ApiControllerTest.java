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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

        Boolean response = restTemplate.postForObject("/api/make-reservation",
                new MakeReservation(
                        offer.reservationId, offer.arrival, offer.departure,
                        "John Doe", "john@example.com"),
                Boolean.class);

        assertThat(response, is(true));
        waitForProjectionsToUpdate();
        test_reservations();
        test_reservationById();
    }

    // XXX: implement the following as dependent tests

    public void test_reservations() {
        // TODO: figure out how to parameterize the list element type
        List<Map<String, Object>> reservations = restTemplate.getForObject("/api/reservations", List.class);

        assertThat("reservations", reservations, is(not(empty())));
        String reservationId = this.reservationId.toString();
        assertThat("reservation " + reservationId, reservations.stream()
                .filter(reservation -> reservation.get("reservationId").equals(reservationId))
                .findFirst(), is(notNullValue()));
    }

    public void test_reservationById() {
        ReservationDto reservation = restTemplate.getForObject("/api/reservations/{id}", ReservationDto.class, reservationId);

        assertThat("reservation", reservation, is(notNullValue()));
        assertThat("reservationId", reservation.reservationId, is(reservationId));
    }

    @Test
    public void create_room() {
        Boolean response = restTemplate.postForObject("/api/create-room",
                new CreateRoom(roomId, "123"),
                Boolean.class);

        assertThat(response, is(true));
        waitForProjectionsToUpdate();
        test_rooms();
        test_capacityByDate();
    }

    public void test_rooms() {
        // TODO: figure out how to parameterize the list element type
        List<Map<String, Object>> rooms = restTemplate.getForObject("/api/rooms", List.class);

        assertThat("rooms", rooms, is(not(empty())));
        String roomId = this.roomId.toString();
        assertThat("room " + roomId, rooms.stream()
                .filter(room -> room.get("roomId").equals(roomId))
                .findFirst(), is(notNullValue()));
    }

    public void test_capacityByDate() {
        LocalDate date = LocalDate.now();

        CapacityDto capacity = restTemplate.getForObject("/api/capacity/{date}", CapacityDto.class, date);

        assertThat("date", capacity.date, is(date));
        assertThat("capacity", capacity.capacity, is(greaterThan(0)));
        assertThat("reserved", capacity.reserved, is(notNullValue()));
    }

    @Test
    public void test_capacityByDateRange() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(2); // 3 days inclusive

        List<Map<String, Object>> capacities = restTemplate.getForObject("/api/capacity/{start}/{end}", List.class, start, end);

        assertThat("capacities", capacities, hasSize(3));
    }

    private static void waitForProjectionsToUpdate() {
        // XXX: use a more reliable mechanism to give the client a consistent view
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
