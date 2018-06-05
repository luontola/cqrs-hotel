// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.Struct;

import java.util.UUID;

public class RoomAssigned extends Struct implements Event {

    public final UUID reservationId;
    public final UUID roomId;
    public final String roomNumber;

    @JsonCreator
    public RoomAssigned(@JsonProperty("reservationId") UUID reservationId,
                        @JsonProperty("roomId") UUID roomId,
                        @JsonProperty("roomNumber") String roomNumber) {
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
    }
}
