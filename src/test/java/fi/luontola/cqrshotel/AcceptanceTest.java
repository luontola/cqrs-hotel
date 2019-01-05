// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.consistency.ReadModelNotUpToDateException;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.eventstore.InMemoryEventStore;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.pricing.RandomPricingEngine;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.FindReservationById;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRange;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@Tag("fast")
public class AcceptanceTest {

    private static final Duration testTimeout = Duration.ofSeconds(2);
    private static final Duration assertionTimeout = Duration.ofSeconds(1);

    private static final UUID reservationId = UUID.randomUUID();
    private static final LocalDate arrival = LocalDate.now();
    private static final LocalDate departure = arrival.plusDays(2);

    private final Clock clock = Clock.systemDefaultZone();
    private final EventStore eventStore = new InMemoryEventStore();
    private final PricingEngine pricing = new RandomPricingEngine(clock);
    private final ObservedPosition observedPosition = new ObservedPosition(assertionTimeout);
    private final Core core = new Core(eventStore, pricing, clock, observedPosition);

    @Test
    public void making_a_reservation() {
        assertTimeoutPreemptively(testTimeout, () -> {
            core.handle(new CreateRoom(UUID.randomUUID(), "101"));
            assertRoomAvailable("before reservation", is(true));

            var offer = (ReservationOffer) core.handle(new SearchForAccommodation(reservationId, arrival, departure));
            assertThat("total price", offer.totalPrice, is(notNullValue()));

            core.handle(new MakeReservation(reservationId, arrival, departure, "John Doe", "john@example.com"));
            eventually(() -> assertRoomAvailable("after reservation", is(false)));
            eventually(() -> assertThat("room is assigned to reservation", reservation().roomNumber, is("101")));
        });
    }

    private ReservationDto reservation() {
        return (ReservationDto) core.handle(new FindReservationById(reservationId));
    }


    // helpers

    private void assertRoomAvailable(String message, Matcher<Boolean> matcher) {
        var rooms = (RoomAvailabilityDto[]) core.handle(new GetAvailabilityByDateRange(arrival, departure));
        assertThat(rooms, is(arrayWithSize(1)));
        assertThat("room available " + message + "?", rooms[0].available, matcher);
    }

    /**
     * Makes assertions about eventually consistent facts, typically when dealing with process managers.
     */
    private void eventually(Runnable assertion) {
        var timeout = Instant.now().plus(assertionTimeout);
        AssertionError assertionError = null;
        do {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                // make the next query wait for the projection to be updated with new events
                observedPosition.observe(observedPosition.get() + 1);
                assertionError = e;
            } catch (ReadModelNotUpToDateException e) {
                // assertion timed out while waiting for new events
                if (assertionError != null) {
                    assertionError.addSuppressed(e);
                    throw assertionError;
                } else {
                    throw e;
                }
            }
        } while (Instant.now().isBefore(timeout));
    }
}
