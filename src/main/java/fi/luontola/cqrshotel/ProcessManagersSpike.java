// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Dispatcher;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ProcessManagersSpike extends AnnotatedProjection {

    // TODO: spike code

    private static final Logger log = LoggerFactory.getLogger(ProcessManagersSpike.class);

    private final RoomAvailabilityView roomAvailabilityView;
    private final Dispatcher dispatcher;

    public ProcessManagersSpike(RoomAvailabilityView roomAvailabilityView, Dispatcher dispatcher) {
        this.roomAvailabilityView = roomAvailabilityView;
        this.dispatcher = dispatcher;
    }

    @EventListener
    public void apply(ReservationInitiated event) {
        log.info("received " + event);
        Instant start = event.checkInTime.toInstant();
        Instant end = event.checkOutTime.toInstant();
        RoomAvailabilityDto room = findAvailableRoom(start, end);
        if (room != null) {
            dispatch(new OccupyRoom(room.roomId, start, end, event.reservationId));
        } else {
            log.info("no rooms available for " + event);
        }
    }

    private RoomAvailabilityDto findAvailableRoom(Instant start, Instant end) {
        return roomAvailabilityView.getAvailabilityForAllRooms(start, end).stream()
                .filter(room -> room.available)
                .findAny()
                .orElse(null);
    }

    @EventListener
    public void apply(RoomOccupied event) {
        log.info("received " + event);
    }

    private void dispatch(Command message) {
        // TODO: spike code, should not dispatch the message on replay
        log.info("dispatch " + message);
        dispatcher.dispatch(message);
    }
}
