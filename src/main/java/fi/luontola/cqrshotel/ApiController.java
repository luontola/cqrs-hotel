// Copyright © 2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.CompositeHandler;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.pricing.PricingEngine;
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

    private final CompositeHandler<Command, Void> commandHandler;
    private final CompositeHandler<Query, Object> queryHandler;

    public ApiController(EventStore eventStore, PricingEngine pricing, Clock clock) {
        ReservationRepo reservationRepo = new ReservationRepo(eventStore);

        CompositeHandler<Command, Void> commandHandler = new CompositeHandler<>();
        commandHandler.register(SearchForAccommodation.class, new SearchForAccommodationHandler(reservationRepo, pricing, clock));
        commandHandler.register(MakeReservation.class, new MakeReservationHandler(reservationRepo, clock));
        this.commandHandler = commandHandler;

        CompositeHandler<Query, Object> queryHandler = new CompositeHandler<>();
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
        return (ReservationOffer) queryHandler.handle(command);
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public Boolean makeReservation(@RequestBody MakeReservation command) {
        commandHandler.handle(command);
        return true;
    }
}
