// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.queries.CapacityDto;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.stub;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

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
    private String observedPosition = null;

    @Before
    public void initMocks() {
        stub(pricingEngine.getAccommodationPrice(any(LocalDate.class)))
                .toReturn(Optional.of(pricePerDay));
    }

    @Test
    public void home() {
        String response = getForObject("/api", String.class);

        assertThat(response, containsString("CQRS Hotel"));
    }

    @Test
    public void search_for_accommodation() {
        ReservationOffer offer = postForObject("/api/search-for-accommodation",
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
        ReservationOffer offer = postForObject("/api/search-for-accommodation",
                new SearchForAccommodation(reservationId, arrival, departure),
                ReservationOffer.class);

        postForObject("/api/make-reservation",
                new MakeReservation(
                        offer.reservationId, offer.arrival, offer.departure,
                        "John Doe", "john@example.com"),
                Object.class);

        test_reservations();
        test_reservationById();
    }

    // XXX: implement the following as dependent tests

    public void test_reservations() {
        ReservationDto[] reservations = getForObject("/api/reservations", ReservationDto[].class);

        assertThat("reservations", reservations, is(not(emptyArray())));
        assertThat("reservation " + reservationId, Stream.of(reservations)
                .filter(reservation -> reservation.reservationId.equals(reservationId))
                .findFirst(), is(notNullValue()));
    }

    public void test_reservationById() {
        ReservationDto reservation = getForObject("/api/reservations/{id}", ReservationDto.class, reservationId);

        assertThat("reservation", reservation, is(notNullValue()));
        assertThat("reservationId", reservation.reservationId, is(reservationId));
    }

    @Test
    public void create_room() {
        postForObject("/api/create-room",
                new CreateRoom(roomId, "123"),
                Object.class);

        test_rooms();
        test_capacityByDate();
    }

    public void test_rooms() {
        RoomDto[] rooms = getForObject("/api/rooms", RoomDto[].class);

        assertThat("rooms", rooms, is(not(emptyArray())));
        assertThat("room " + roomId, Stream.of(rooms)
                .filter(room -> room.roomId.equals(roomId))
                .findFirst(), is(notNullValue()));
    }

    public void test_capacityByDate() {
        LocalDate date = LocalDate.now();

        CapacityDto capacity = getForObject("/api/capacity/{date}", CapacityDto.class, date);

        assertThat("date", capacity.date, is(date));
        assertThat("capacity", capacity.capacity, is(greaterThan(0)));
        assertThat("reserved", capacity.reserved, is(notNullValue()));
    }

    @Test
    public void test_capacityByDateRange() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(2); // 3 days inclusive

        CapacityDto[] capacities = getForObject("/api/capacity/{start}/{end}", CapacityDto[].class, start, end);

        assertThat("capacities", capacities, arrayWithSize(3));
    }


    // helpers

    private <T> T postForObject(String url, Object request, Class<T> responseType, Object... urlVariables) {
        ResponseEntity<T> response = restTemplate.exchange(url, POST, new HttpEntity<>(request, headers()), responseType, urlVariables);
        assert2xxSuccessful(response);
        rememberObservedPosition(response);
        return response.getBody();
    }

    private <T> T getForObject(String url, Class<T> responseType, Object... urlVariables) {
        ResponseEntity<T> response = restTemplate.exchange(url, GET, new HttpEntity<>(headers()), responseType, urlVariables);
        assert2xxSuccessful(response);
        return response.getBody();
    }

    private void rememberObservedPosition(ResponseEntity<?> response) {
        String observedPosition = response.getHeaders().getFirst(ObservedPosition.HTTP_HEADER);
        if (observedPosition != null) {
            this.observedPosition = observedPosition;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (observedPosition != null) {
            headers.set(ObservedPosition.HTTP_HEADER, observedPosition);
        }
        return headers;
    }

    private static void assert2xxSuccessful(ResponseEntity<?> response) {
        HttpStatus statusCode = response.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            throw new AssertionError("HTTP " + statusCode + " " + statusCode.getReasonPhrase());
        }
    }
}
