// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class UseRoomForReservation extends Struct implements Command {

    public final UUID reservationId;
    public final UUID roomId;

    @JsonCreator
    public UseRoomForReservation(@JsonProperty("reservationId") UUID reservationId,
                                 @JsonProperty("roomId") UUID roomId) {
        this.reservationId = reservationId;
        this.roomId = roomId;
    }
}
