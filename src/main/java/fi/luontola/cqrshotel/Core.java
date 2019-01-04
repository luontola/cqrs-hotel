// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.queries.CapacityDto;
import fi.luontola.cqrshotel.capacity.queries.CapacityView;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDate;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateHandler;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateRange;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateRangeHandler;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.CompositeHandler;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.framework.MessageGateway;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.consistency.UpdateObservedPositionAfterCommit;
import fi.luontola.cqrshotel.framework.consistency.WaitForProjectionToUpdate;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.processes.ProcessManagers;
import fi.luontola.cqrshotel.framework.processes.ProcessManagersProjectionAdapter;
import fi.luontola.cqrshotel.framework.processes.ProcessRepo;
import fi.luontola.cqrshotel.framework.projections.InMemoryProjection;
import fi.luontola.cqrshotel.framework.projections.Projection;
import fi.luontola.cqrshotel.framework.projections.UpdatableProjection;
import fi.luontola.cqrshotel.framework.projections.UpdateProjectionsAfterHandling;
import fi.luontola.cqrshotel.framework.util.WorkersPool;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.ReservationProcess;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.AssignRoom;
import fi.luontola.cqrshotel.reservation.commands.AssignRoomHandler;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationCommandHandler;
import fi.luontola.cqrshotel.reservation.queries.FindAllReservations;
import fi.luontola.cqrshotel.reservation.queries.FindAllReservationsHandler;
import fi.luontola.cqrshotel.reservation.queries.FindReservationById;
import fi.luontola.cqrshotel.reservation.queries.FindReservationByIdHandler;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationsView;
import fi.luontola.cqrshotel.reservation.queries.SearchForAccommodationQueryHandler;
import fi.luontola.cqrshotel.room.RoomRepo;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.commands.CreateRoomHandler;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoom;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoomHandler;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.commands.OccupyRoomHandler;
import fi.luontola.cqrshotel.room.queries.FindAllRooms;
import fi.luontola.cqrshotel.room.queries.FindAllRoomsHandler;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRange;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByDateRangeHandler;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByTimeRange;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByTimeRangeHandler;
import fi.luontola.cqrshotel.room.queries.GetRoomById;
import fi.luontola.cqrshotel.room.queries.GetRoomByIdHandler;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import fi.luontola.cqrshotel.room.queries.RoomsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Core {

    private static final Logger log = LoggerFactory.getLogger(Core.class);

    private final EventStore eventStore;
    private final ObservedPosition observedPosition;

    private final List<ProjectionConfig<?>> projections = new ArrayList<>();
    private final WorkersPool projectionsUpdater;

    private final Handler<Command, Commit> commandDispatcher;
    private final Handler<Query, Object> queryDispatcher;

    public Core(EventStore eventStore, PricingEngine pricing, Clock clock, ObservedPosition observedPosition) {
        this.eventStore = eventStore;
        this.observedPosition = observedPosition;
        Publisher publisher = message -> handle(Envelope.newMessage(message));

        // projections

        addInMemoryProjection(new ReservationsView())
                .addQueryHandler(FindAllReservationsHandler::new, FindAllReservations.class, ReservationDto[].class)
                .addQueryHandler(FindReservationByIdHandler::new, FindReservationById.class, ReservationDto.class);

        addInMemoryProjection(new RoomsView())
                .addQueryHandler(GetRoomByIdHandler::new, GetRoomById.class, RoomDto.class)
                .addQueryHandler(FindAllRoomsHandler::new, FindAllRooms.class, RoomDto[].class);

        addInMemoryProjection(new RoomAvailabilityView())
                .addQueryHandler(GetAvailabilityByTimeRangeHandler::new, GetAvailabilityByTimeRange.class, RoomAvailabilityDto[].class)
                .addQueryHandler(GetAvailabilityByDateRangeHandler::new, GetAvailabilityByDateRange.class, RoomAvailabilityDto[].class);

        addInMemoryProjection(new CapacityView())
                .addQueryHandler(GetCapacityByDateHandler::new, GetCapacityByDate.class, CapacityDto.class)
                .addQueryHandler(GetCapacityByDateRangeHandler::new, GetCapacityByDateRange.class, CapacityDto[].class);

        // TODO: spike code, need support for multiple (persisted) process instances
        MessageGateway gateway = message -> {
            // TODO: should this be asynchronous?
            try {
                handle(message);
            } catch (Throwable t) {
                log.error("Uncaught exception in handling " + message, t);
            }
        };
        addInMemoryProjection(new ProcessManagersProjectionAdapter( // TODO: smarter projection, keep track of individual processes
                new ProcessManagers(new ProcessRepo(), gateway)
                        .register(ReservationProcess.class, ReservationProcess::entryPoint)));

        this.projectionsUpdater = new WorkersPool(projections.stream()
                .map(p -> (Runnable) p.updater::update)
                .collect(Collectors.toList()));

        // queries

        var queries = new CompositeHandler<Query, Object>();
        for (var projection : projections) {
            projection.registerQueryHandlers(queries);
        }
        queries.register(SearchForAccommodation.class, new SearchForAccommodationQueryHandler(eventStore, clock));
        this.queryDispatcher = queries;

        // commands

        var commands = new CompositeHandler<Command, Commit>();

        var reservationRepo = new ReservationRepo(eventStore);
        commands.register(SearchForAccommodation.class, new SearchForAccommodationCommandHandler(reservationRepo, pricing, clock));
        commands.register(MakeReservation.class, new MakeReservationHandler(reservationRepo, clock));
        commands.register(AssignRoom.class, new AssignRoomHandler(reservationRepo, getQueryHandler(GetRoomById.class, RoomDto.class)));

        var roomRepo = new RoomRepo(eventStore);
        commands.register(CreateRoom.class, new CreateRoomHandler(roomRepo));
        commands.register(OccupyRoom.class, new OccupyRoomHandler(roomRepo));
        commands.register(OccupyAnyAvailableRoom.class, new OccupyAnyAvailableRoomHandler(publisher, getQueryHandler(GetAvailabilityByTimeRange.class, RoomAvailabilityDto[].class)));

        this.commandDispatcher = new UpdateObservedPositionAfterCommit(observedPosition,
                new UpdateProjectionsAfterHandling<>(projectionsUpdater, commands));
    }

    private <T extends Projection> ProjectionConfig<T> addInMemoryProjection(T projection) {
        var config = new ProjectionConfig<>(projection, new InMemoryProjection(projection, eventStore));
        projections.add(config);
        return config;
    }

    @PostConstruct
    public void startup() {
        // FIXME: Spring calls this before Flyway is initialized,
        //  which results in error when the database tables are missing.
        //  Must configure Spring initialization order, or better, get rid of Spring.
        projectionsUpdater.updateAll();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        projectionsUpdater.shutdown(Duration.ofSeconds(10));
    }

    public Object handle(Message message) {
        return handle(Envelope.newMessage(message));
    }

    public Object handle(Envelope<?> message) {
        Envelope.setContext(message);
        try {
            log.info("Handle {} - {}", message.payload, message.metaToString());
            // XXX: It's possible for a message to implement both Command and Query,
            // in which case the command will first execute and then the query
            // will observe the data produced by the command.
            Object result = null;
            if (message.payload instanceof Command) {
                commandDispatcher.handle((Command) message.payload);
                result = Collections.emptyMap();
            }
            if (message.payload instanceof Query) {
                result = queryDispatcher.handle((Query) message.payload);
            }
            return result;
        } finally {
            Envelope.resetContext();
        }
    }

    public StatusPage getStatus() {
        return StatusPage.build(eventStore, projections);
    }

    @SuppressWarnings("unchecked")
    private <Q extends Query, R> Handler<Q, R> getQueryHandler(Class<Q> queryType, Class<R> responseType) {
        for (var projection : projections) {
            for (var queryHandler : projection.queryHandlers) {
                if (queryHandler.queryType.equals(queryType) && queryHandler.responseType.equals(responseType)) {
                    return (Handler<Q, R>) queryHandler.handler;
                }
            }
        }
        throw new IllegalArgumentException("Query handler not found: " + queryType + " -> " + responseType);
    }

    public class ProjectionConfig<P extends Projection> {
        public final P projection;
        public final UpdatableProjection updater;
        public final List<QueryHandlerConfig<?, ?>> queryHandlers = new ArrayList<>();

        public ProjectionConfig(P projection, UpdatableProjection updater) {
            this.projection = projection;
            this.updater = updater;
        }

        public <Q extends Query, R> ProjectionConfig<P> addQueryHandler(Function<P, Handler<Q, R>> handlerFactory, Class<Q> query, Class<R> response) {
            queryHandlers.add(new QueryHandlerConfig<>(query, response, handlerFactory.apply(projection)));
            return this;
        }

        public void registerQueryHandlers(CompositeHandler<Query, Object> registry) {
            for (var queryHandler : queryHandlers) {
                queryHandler.register(registry, updater);
            }
        }
    }

    public class QueryHandlerConfig<Q extends Query, R> {
        public final Class<Q> queryType;
        public final Class<R> responseType;
        public final Handler<Q, R> handler;

        public QueryHandlerConfig(Class<Q> queryType, Class<R> responseType, Handler<Q, R> handler) {
            this.queryType = queryType;
            this.responseType = responseType;
            this.handler = handler;
        }

        public void register(CompositeHandler<Query, Object> registry, UpdatableProjection updater) {
            registry.register(queryType, new WaitForProjectionToUpdate<>(updater, observedPosition, handler));
        }
    }
}
