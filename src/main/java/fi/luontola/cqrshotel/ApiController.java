// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Handles;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.reservation.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.events.ReservationInitialized;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    private final EventStore eventStore = new InMemoryEventStore();
    private final ReservationRepo reservationRepo = new ReservationRepo(eventStore);
    private final Handles<Command> commandHandler = (Handles) new MakeReservationHandler(reservationRepo);

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/dummy", method = GET)
    public List<String> dummy() {
        return Arrays.asList("foo", "bar", "gazonk");
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public void makeReservation(@RequestBody MakeReservation command) {
        // TODO
        eventStore.saveEvents(command.reservationId,
                Arrays.asList(new ReservationInitialized(command.reservationId)),
                EventStore.NEW_STREAM);
        commandHandler.handle(command);
    }
}
