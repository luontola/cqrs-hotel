// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class FindReservationById extends Struct implements Query {

    public final UUID reservationId;

    @JsonCreator
    public FindReservationById(@JsonProperty("reservationId") UUID reservationId) {
        this.reservationId = reservationId;
    }
}
