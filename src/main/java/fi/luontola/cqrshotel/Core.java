// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.queries.CapacityView;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDate;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateHandler;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateRange;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateRangeHandler;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.CompositeHandler;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.framework.ProjectionsUpdater;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.framework.UpdateProjectionsAfterHandling;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.consistency.UpdateObservedPositionAfterCommit;
import fi.luontola.cqrshotel.framework.consistency.WaitForProjectionToUpdate;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationCommandHandler;
import fi.luontola.cqrshotel.reservation.queries.FindAllReservations;
import fi.luontola.cqrshotel.reservation.queries.FindAllReservationsHandler;
import fi.luontola.cqrshotel.reservation.queries.FindReservationById;
import fi.luontola.cqrshotel.reservation.queries.FindReservationByIdHandler;
import fi.luontola.cqrshotel.reservation.queries.ReservationsView;
import fi.luontola.cqrshotel.reservation.queries.SearchForAccommodationQueryHandler;
import fi.luontola.cqrshotel.room.RoomRepo;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.commands.CreateRoomHandler;
import fi.luontola.cqrshotel.room.queries.FindAllRooms;
import fi.luontola.cqrshotel.room.queries.FindAllRoomsHandler;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRange;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRangeHandler;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;
import fi.luontola.cqrshotel.room.queries.RoomsView;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Core {

    private final EventStore eventStore;
    private final Handler<Command, Commit> commandHandler;
    private final Handler<Query, Object> queryHandler;
    private final ProjectionsUpdater projectionsUpdater;

    private final List<Projection> projections;
    final ReservationsView reservationsView;
    final RoomsView roomsView;
    final RoomAvailabilityView roomAvailabilityView;
    final CapacityView capacityView;

    public Core(EventStore eventStore, PricingEngine pricing, Clock clock, ObservedPosition observedPosition) {
        this.eventStore = eventStore;
        ReservationRepo reservationRepo = new ReservationRepo(eventStore);
        RoomRepo roomRepo = new RoomRepo(eventStore);

        projections = Arrays.asList(
                reservationsView = new ReservationsView(eventStore),
                roomsView = new RoomsView(eventStore),
                roomAvailabilityView = new RoomAvailabilityView(eventStore),
                capacityView = new CapacityView(eventStore)
        );
        projectionsUpdater = new ProjectionsUpdater(projections);

        CompositeHandler<Command, Commit> commandHandler = new CompositeHandler<>();
        commandHandler.register(SearchForAccommodation.class, new SearchForAccommodationCommandHandler(reservationRepo, pricing, clock));
        commandHandler.register(MakeReservation.class, new MakeReservationHandler(reservationRepo, clock));
        commandHandler.register(CreateRoom.class, new CreateRoomHandler(roomRepo));
        this.commandHandler = new UpdateObservedPositionAfterCommit(observedPosition,
                new UpdateProjectionsAfterHandling<>(projectionsUpdater, commandHandler));

        CompositeHandler<Query, Object> queryHandler = new CompositeHandler<>();
        queryHandler.register(SearchForAccommodation.class,
                new SearchForAccommodationQueryHandler(eventStore, clock));
        queryHandler.register(FindAllReservations.class,
                new WaitForProjectionToUpdate<>(reservationsView, observedPosition,
                        new FindAllReservationsHandler(reservationsView)));
        queryHandler.register(FindReservationById.class,
                new WaitForProjectionToUpdate<>(reservationsView, observedPosition,
                        new FindReservationByIdHandler(reservationsView)));
        queryHandler.register(FindAllRooms.class,
                new WaitForProjectionToUpdate<>(roomsView, observedPosition,
                        new FindAllRoomsHandler(roomsView)));
        queryHandler.register(GetAvailabilityByDateRange.class,
                new WaitForProjectionToUpdate<>(roomAvailabilityView, observedPosition,
                        new GetAvailabilityByDateRangeHandler(roomAvailabilityView)));
        queryHandler.register(GetCapacityByDate.class,
                new WaitForProjectionToUpdate<>(capacityView, observedPosition,
                        new GetCapacityByDateHandler(capacityView)));
        queryHandler.register(GetCapacityByDateRange.class,
                new WaitForProjectionToUpdate<>(capacityView, observedPosition,
                        new GetCapacityByDateRangeHandler(capacityView)));
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

    public Object handle(Message message) {
        Object result = null;
        if (message instanceof Command) {
            commandHandler.handle((Command) message);
            result = Collections.emptyMap();
        }
        if (message instanceof Query) {
            result = queryHandler.handle((Query) message);
        }
        return result;
    }

    public StatusPage getStatus() {
        return StatusPage.build(eventStore, projections);
    }
}
