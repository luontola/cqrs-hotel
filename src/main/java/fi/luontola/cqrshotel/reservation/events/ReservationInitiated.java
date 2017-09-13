// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ReservationInitiated extends Struct implements Event {

    public final UUID reservationId;
    public final ZonedDateTime checkInTime;
    public final ZonedDateTime checkOutTime;

    @JsonCreator
    public ReservationInitiated(@JsonProperty("reservationId") UUID reservationId,
                                @JsonProperty("checkInTime") ZonedDateTime checkInTime,
                                @JsonProperty("checkOutTime") ZonedDateTime checkOutTime) {
        this.reservationId = reservationId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
}
