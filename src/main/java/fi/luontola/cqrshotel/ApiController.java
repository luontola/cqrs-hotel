// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.*;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.pricing.RandomPricingEngine;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationHandler;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.reservation.queries.SearchForAccommodationQuery;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    private final MessageRouter<Command> commandHandler;
    private final QueryRouter<Query> queryHandler;

    public ApiController() {
        EventStore eventStore = new InMemoryEventStore();
        ReservationRepo reservationRepo = new ReservationRepo(eventStore);
        Clock clock = Clock.systemDefaultZone();
        PricingEngine pricing = new RandomPricingEngine(clock);

        MessageRouter<Command> commandHandler = new MessageRouter<>();
        commandHandler.register(SearchForAccommodation.class, new SearchForAccommodationHandler(reservationRepo, pricing, clock));
        commandHandler.register(MakeReservation.class, new MakeReservationHandler(reservationRepo));
        this.commandHandler = commandHandler;

        QueryRouter<Query> queryHandler = new QueryRouter<>();
        queryHandler.register(SearchForAccommodation.class, new SearchForAccommodationQuery(eventStore, clock));
        this.queryHandler = queryHandler;
    }

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/search-for-accommodation", method = POST)
    public ReservationOffer findAvailableRoom(@RequestBody SearchForAccommodation command) {
        commandHandler.handle(command);
        return (ReservationOffer) queryHandler.query(command);
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public Boolean makeReservation(@RequestBody MakeReservation command) {
        commandHandler.handle(command);
        return true;
    }
}
