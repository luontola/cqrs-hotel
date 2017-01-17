// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;

import java.time.Instant;
import java.util.UUID;

public class ReservationMade extends Struct implements Event {

    public final UUID reservationId;
    public final Instant checkInTime;
    public final Instant checkOutTime;

    @JsonCreator
    public ReservationMade(@JsonProperty("reservationId") UUID reservationId,
                           @JsonProperty("checkInTime") Instant checkInTime,
                           @JsonProperty("checkOutTime") Instant checkOutTime) {
        this.reservationId = reservationId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
}
