// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.FakeEventStore;
import fi.luontola.cqrshotel.framework.Handles;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitialized;
import fi.luontola.cqrshotel.reservation.events.ReservationMade;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ReservationTest {

    private UUID id = UUID.randomUUID();
    private FakeEventStore eventStore = new FakeEventStore();
    private ReservationRepo repository = new ReservationRepo(eventStore);
    private Handles commandHandler = new MakeReservationHandler(repository);

    @Test
    public void make_reservation() {
        LocalDate startDate = LocalDate.of(2000, 1, 2);
        LocalDate endDate = LocalDate.of(2001, 3, 4);

        given(new ReservationInitialized(id));
        when(new MakeReservation(id, startDate, endDate, "John Doe", "john@example.com"));
        then(new ContactInformationUpdated(id, "John Doe", "john@example.com"),
                new ReservationMade(id,
                        ZonedDateTime.of(startDate, Reservation.CHECK_IN_TIME, Reservation.TIMEZONE).toInstant(),
                        ZonedDateTime.of(endDate, Reservation.CHECK_OUT_TIME, Reservation.TIMEZONE).toInstant()));
    }

    public void given(Event... events) {
        eventStore.existing = Arrays.asList(events);
    }

    public void when(Command command) {
        commandHandler.handle(command);
    }

    public void then(Event... expectedEvents) {
        assertThat("produced events", eventStore.produced, is(Arrays.asList(expectedEvents)));
    }
}
