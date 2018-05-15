// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.EventListeners;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.reservation.commands.AssignRoom;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoom;
import fi.luontola.cqrshotel.room.events.RoomOccupied;

public class ReservationProcess {

    private final Publisher publisher;
    private final EventListeners eventListeners;

    public ReservationProcess(Publisher publisher) {
        this.publisher = publisher;
        eventListeners = EventListeners.of(this);
    }

    public void handle(Event event) {
        eventListeners.send(event);
    }

    @EventListener
    public void handle(ReservationInitiated event) {
        publisher.publish(new OccupyAnyAvailableRoom(event.checkInTime.toInstant(), event.checkOutTime.toInstant(), event.reservationId));
    }

    @EventListener
    public void handle(RoomOccupied event) {
        publisher.publish(new AssignRoom(event.occupant, event.roomId));
    }
}
