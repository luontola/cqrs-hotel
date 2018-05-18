// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.pricing.RandomPricingEngine;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRange;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Category(FastTests.class)
public class AcceptanceTest {

    private static final UUID reservationId = UUID.randomUUID();
    private static final LocalDate arrival = LocalDate.now();
    private static final LocalDate departure = arrival.plusDays(1);

    private final Clock clock = Clock.systemDefaultZone();
    private final EventStore eventStore = new InMemoryEventStore();
    private final PricingEngine pricing = new RandomPricingEngine(clock);
    private final ObservedPosition observedPosition = new ObservedPosition(Duration.ofSeconds(5));
    private final Core core = new Core(eventStore, pricing, clock, observedPosition);

    @Rule
    public final Timeout timeout = Timeout.seconds(2);
    public static final Duration assertionTimeout = Duration.ofSeconds(1);
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void making_a_reservation() {
        core.handle(new CreateRoom(UUID.randomUUID(), "101"));
        assertRoomAvailable(true);

        ReservationOffer offer = (ReservationOffer) core.handle(new SearchForAccommodation(reservationId, arrival, departure));
        assertThat("total price", offer.totalPrice, is(notNullValue()));

        core.handle(new MakeReservation(reservationId, arrival, departure, "John Doe", "john@example.com"));
        eventually(() -> assertRoomAvailable(false));
    }


    // helpers

    private void assertRoomAvailable(boolean expected) {
        RoomAvailabilityDto[] rooms = (RoomAvailabilityDto[]) core.handle(new GetAvailabilityByDateRange(arrival, departure));
        assertThat(rooms, is(arrayWithSize(1)));
        assertThat("room available", rooms[0].available, is(expected));
    }

    /**
     * Makes assertions about eventually consistent facts, typically when dealing with process managers.
     */
    private void eventually(Runnable assertion) {
        Instant timeout = Instant.now().plus(assertionTimeout);
        do {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                // make the next query wait for the projection to be updated with new events
                observedPosition.observe(observedPosition.get() + 1);
            }
        } while (Instant.now().isBefore(timeout));
    }
}
