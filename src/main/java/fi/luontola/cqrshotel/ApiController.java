// Copyright © 2016-2017 Esko Luontola
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
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationCommandHandler;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.reservation.queries.ReservationsView;
import fi.luontola.cqrshotel.reservation.queries.SearchForAccommodationQueryHandler;
import fi.luontola.cqrshotel.room.RoomRepo;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.commands.CreateRoomHandler;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import fi.luontola.cqrshotel.room.queries.RoomsView;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    private final CompositeHandler<Command, Void> commandHandler;
    private final CompositeHandler<Query, Object> queryHandler;
    private final ReservationsView reservationsView;
    private final RoomsView roomsView;

    public ApiController(EventStore eventStore, PricingEngine pricing, Clock clock) {
        ReservationRepo reservationRepo = new ReservationRepo(eventStore);
        RoomRepo roomRepo = new RoomRepo(eventStore);

        CompositeHandler<Command, Void> commandHandler = new CompositeHandler<>();
        commandHandler.register(SearchForAccommodation.class, new SearchForAccommodationCommandHandler(reservationRepo, pricing, clock));
        commandHandler.register(MakeReservation.class, new MakeReservationHandler(reservationRepo, clock));
        commandHandler.register(CreateRoom.class, new CreateRoomHandler(roomRepo));
        // TODO: trigger updating projections after processing any command
        this.commandHandler = commandHandler;

        CompositeHandler<Query, Object> queryHandler = new CompositeHandler<>();
        queryHandler.register(SearchForAccommodation.class, new SearchForAccommodationQueryHandler(eventStore, clock));
        this.queryHandler = queryHandler;

        reservationsView = new ReservationsView(eventStore);
        roomsView = new RoomsView(eventStore);
    }

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/search-for-accommodation", method = POST)
    public ReservationOffer searchForAccommodation(@RequestBody SearchForAccommodation command) {
        commandHandler.handle(command);
        return (ReservationOffer) queryHandler.handle(command);
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public Boolean makeReservation(@RequestBody MakeReservation command) {
        commandHandler.handle(command);
        return true;
    }

    @RequestMapping(path = "/api/reservations", method = GET)
    public List<ReservationDto> reservations() {
        reservationsView.update(); // TODO: update asynchronously when events are created
        return reservationsView.findAll();
    }

    @RequestMapping(path = "/api/create-room", method = POST)
    public Boolean createRoom(@RequestBody CreateRoom command) {
        commandHandler.handle(command);
        return true;
    }

    @RequestMapping(path = "/api/rooms", method = GET)
    public List<RoomDto> rooms() {
        roomsView.update(); // TODO: update asynchronously when events are created
        return roomsView.findAll();
    }
}
