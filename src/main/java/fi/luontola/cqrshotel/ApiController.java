// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.CapacityDto;
import fi.luontola.cqrshotel.capacity.CapacityView;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.CompositeHandler;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.ProjectionsUpdater;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.framework.UpdateProjectionsAfterHandling;
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
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    public static final String OBSERVED_POSITION_HEADER = "X-Observed-Position";

    private final Handler<Command, Commit> commandHandler;
    private final Handler<Query, Object> queryHandler;
    private final ProjectionsUpdater projectionsUpdater;

    private final ReservationsView reservationsView;
    private final RoomsView roomsView;
    private final CapacityView capacityView;

    public ApiController(EventStore eventStore, PricingEngine pricing, Clock clock) {
        ReservationRepo reservationRepo = new ReservationRepo(eventStore);
        RoomRepo roomRepo = new RoomRepo(eventStore);

        projectionsUpdater = new ProjectionsUpdater(
                reservationsView = new ReservationsView(eventStore),
                roomsView = new RoomsView(eventStore),
                capacityView = new CapacityView(eventStore)
        );

        CompositeHandler<Command, Commit> commandHandler = new CompositeHandler<>();
        commandHandler.register(SearchForAccommodation.class, new SearchForAccommodationCommandHandler(reservationRepo, pricing, clock));
        commandHandler.register(MakeReservation.class, new MakeReservationHandler(reservationRepo, clock));
        commandHandler.register(CreateRoom.class, new CreateRoomHandler(roomRepo));
        this.commandHandler = new UpdateProjectionsAfterHandling<>(projectionsUpdater, commandHandler);

        CompositeHandler<Query, Object> queryHandler = new CompositeHandler<>();
        queryHandler.register(SearchForAccommodation.class, new SearchForAccommodationQueryHandler(eventStore, clock));
        this.queryHandler = queryHandler;
    }

    @PostConstruct
    public void startup() {
        projectionsUpdater.updateAll();
    }

    @PreDestroy
    public void shutdown() {
        projectionsUpdater.shutdown();
    }

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/search-for-accommodation", method = POST)
    public ReservationOffer searchForAccommodation(@RequestBody SearchForAccommodation command) {
        Commit commit = commandHandler.handle(command);
        waitForProjectionsToUpdate(commit.committedPosition);
        return (ReservationOffer) queryHandler.handle(command);
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public Commit makeReservation(@RequestBody MakeReservation command) {
        return commandHandler.handle(command);
    }

    @RequestMapping(path = "/api/reservations", method = GET)
    public List<ReservationDto> reservations(@RequestHeader HttpHeaders headers) {
        waitForProjectionsToUpdate(getObservedPosition(headers));
        return reservationsView.findAll();
    }

    @RequestMapping(path = "/api/reservations/{reservationId}", method = GET)
    public ReservationDto reservationById(@PathVariable String reservationId,
                                          @RequestHeader HttpHeaders headers) {
        waitForProjectionsToUpdate(getObservedPosition(headers));
        return reservationsView.findById(UUID.fromString(reservationId));
    }

    @RequestMapping(path = "/api/create-room", method = POST)
    public Commit createRoom(@RequestBody CreateRoom command) {
        return commandHandler.handle(command);
    }

    @RequestMapping(path = "/api/rooms", method = GET)
    public List<RoomDto> rooms(@RequestHeader HttpHeaders headers) {
        waitForProjectionsToUpdate(getObservedPosition(headers));
        return roomsView.findAll();
    }

    @RequestMapping(path = "/api/capacity/{date}", method = GET)
    public CapacityDto capacityByDate(@PathVariable String date,
                                      @RequestHeader HttpHeaders headers) {
        waitForProjectionsToUpdate(getObservedPosition(headers));
        return capacityView.getCapacityByDate(LocalDate.parse(date));
    }

    @RequestMapping(path = "/api/capacity/{start}/{end}", method = GET)
    public List<CapacityDto> capacityByDateRange(@PathVariable String start,
                                                 @PathVariable String end,
                                                 @RequestHeader HttpHeaders headers) {
        waitForProjectionsToUpdate(getObservedPosition(headers));
        return capacityView.getCapacityByDateRange(LocalDate.parse(start), LocalDate.parse(end));
    }


    // helpers

    private static long getObservedPosition(HttpHeaders headers) {
        String value = headers.getFirst(OBSERVED_POSITION_HEADER);
        return value == null ? 0 : Long.parseLong(value);
    }

    private void waitForProjectionsToUpdate(long observedPosition) {
        // XXX: use a more reliable mechanism to give the client a consistent view
        if (observedPosition > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
