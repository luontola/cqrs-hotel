// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Handles;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.events.ReservationInitialized;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import org.javamoney.moneta.Money;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
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

    @RequestMapping(path = "/api/find-available-room", method = GET)
    public ReservationOffer findAvailableRoom(@RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
                                              @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate) {
        // TODO
        ReservationOffer result = new ReservationOffer();
        result.reservationId = UUID.randomUUID();
        result.startDate = startDate;
        result.endDate = endDate;
        result.price = Money.of(ThreadLocalRandom.current().nextInt(50, 150), "EUR");
        return result;
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
