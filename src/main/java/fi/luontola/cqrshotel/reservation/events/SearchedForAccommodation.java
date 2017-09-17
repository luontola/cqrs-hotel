// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;

import java.time.LocalDate;
import java.util.UUID;

public class SearchedForAccommodation extends Struct implements Event {

    public final UUID reservationId;
    public final LocalDate arrival;
    public final LocalDate departure;

    @JsonCreator
    public SearchedForAccommodation(@JsonProperty("reservationId") UUID reservationId,
                                    @JsonProperty("arrival") LocalDate arrival,
                                    @JsonProperty("departure") LocalDate departure) {
        this.reservationId = reservationId;
        this.arrival = arrival;
        this.departure = departure;
    }
}
