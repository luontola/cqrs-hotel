// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.events;

import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class RoomOccupied extends Struct implements Event {

    public final UUID roomId;

    public RoomOccupied(UUID roomId) {
        this.roomId = roomId;
    }
}
