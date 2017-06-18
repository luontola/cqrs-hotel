// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.AggregateRoot;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;

public class Room extends AggregateRoot {

    private boolean occupied = false;

    @EventListener
    private void apply(RoomOccupied event) {
        occupied = true;
    }

    public void createRoom(String number) {
        publish(new RoomCreated(getId(), number));
    }

    public void occupy() {
        if (occupied) {
            throw new RoomAlreadyOccupiedException();
        }
        publish(new RoomOccupied(getId()));
    }
}
