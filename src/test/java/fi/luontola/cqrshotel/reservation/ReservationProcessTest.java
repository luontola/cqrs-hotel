// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.BufferedPublisher;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.reservation.commands.AssignRoom;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoom;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ReservationProcessTest {

    private static final UUID reservationId = UUID.randomUUID();
    private static final UUID roomId = UUID.randomUUID();
    private static final LocalDate arrival = LocalDate.parse("2000-01-01");
    private static final LocalDate departure = LocalDate.parse("2000-01-05");
    private static final ZonedDateTime checkInTime = ZonedDateTime.parse("2000-01-01T14:00Z");
    private static final ZonedDateTime checkOutTime = ZonedDateTime.parse("2000-01-05T10:00Z");

    private final BufferedPublisher publisher = new BufferedPublisher();
    private final ReservationProcess process = new ReservationProcess(publisher);

    // TODO: payment and confirmation process

    @Test
    public void when_reservation_is_initialized_then_an_available_room_is_allocated_for_it() {  // TODO: should be "when confirmed"
        when(new ReservationCreated(reservationId, arrival, departure, checkInTime, checkOutTime));

        then(new OccupyAnyAvailableRoom(checkInTime.toInstant(), checkOutTime.toInstant(), reservationId));
    }

    // TODO: if no room is available then retry

    @Test
    public void when_an_available_room_is_found_then_that_is_assigned_to_the_reservation() {
        given(new ReservationCreated(reservationId, arrival, departure, checkInTime, checkOutTime));

        when(new RoomOccupied(roomId, checkInTime.toInstant(), checkOutTime.toInstant(), reservationId));

        then(new AssignRoom(reservationId, roomId));
    }

    // TODO: if using the room fails then retry


    // helpers

    private void given(Event... events) {
        for (var event : events) {
            process.apply(Envelope.newMessage(event));
        }
    }

    private void when(Event event) {
        publisher.publishedMessages.clear();
        process.apply(Envelope.newMessage(event));
    }

    private void then(Message... expected) {
        assertThat(publisher.publishedMessages, is(Arrays.asList(expected)));
    }
}
