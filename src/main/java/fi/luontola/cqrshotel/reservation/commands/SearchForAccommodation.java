// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.util.Struct;

import java.time.LocalDate;
import java.util.UUID;

public class SearchForAccommodation extends Struct implements Command, Query {

    public final UUID reservationId;
    public final LocalDate startDate;
    public final LocalDate endDate;

    @JsonCreator
    public SearchForAccommodation(@JsonProperty("reservationId") UUID reservationId,
                                  @JsonProperty("startDate") LocalDate startDate,
                                  @JsonProperty("endDate") LocalDate endDate) {
        this.reservationId = reservationId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
