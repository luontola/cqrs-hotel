// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class ReservationInitialized extends Struct implements Event {

    public final UUID reservationId;

    public ReservationInitialized(UUID reservationId) {
        this.reservationId = reservationId;
    }
}
