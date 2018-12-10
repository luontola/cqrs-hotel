// Copyright © 2016-2018 Esko Luontola
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

    @Rule
    public final Timeout timeout = Timeout.seconds(2);
    public static final Duration assertionTimeout = Duration.ofSeconds(1);
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
        core.handle(new CreateRoom(UUID.randomUUID(), "101"));
        assertRoomAvailable("before reservation", is(true));

        ReservationOffer offer = (ReservationOffer) core.handle(new SearchForAccommodation(reservationId, arrival, departure));
        assertThat("total price", offer.totalPrice, is(notNullValue()));

        core.handle(new MakeReservation(reservationId, arrival, departure, "John Doe", "john@example.com"));
        eventually(() -> assertRoomAvailable("after reservation", is(false)));
        eventually(() -> assertThat("room is assigned to reservation", reservation().roomNumber, is("101")));
    }

    private ReservationDto reservation() {
        return (ReservationDto) core.handle(new FindReservationById(reservationId));
    }


    // helpers

    private void assertRoomAvailable(String message, Matcher<Boolean> matcher) {
        RoomAvailabilityDto[] rooms = (RoomAvailabilityDto[]) core.handle(new GetAvailabilityByDateRange(arrival, departure));
        assertThat(rooms, is(arrayWithSize(1)));
        assertThat("room available " + message + "?", rooms[0].available, matcher);
    }

    /**
     * Makes assertions about eventually consistent facts, typically when dealing with process managers.
     */
    private void eventually(Runnable assertion) {
        Instant timeout = Instant.now().plus(assertionTimeout);
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
