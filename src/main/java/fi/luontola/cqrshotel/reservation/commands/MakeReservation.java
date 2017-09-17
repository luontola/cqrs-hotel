// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.util.Struct;

import java.time.LocalDate;
import java.util.UUID;

public class MakeReservation extends Struct implements Command {

    public final UUID reservationId;
    public final LocalDate arrival;
    public final LocalDate departure;
    public final String name;
    public final String email;

    @JsonCreator
    public MakeReservation(@JsonProperty("reservationId") UUID reservationId,
                           @JsonProperty("arrival") LocalDate arrival,
                           @JsonProperty("departure") LocalDate departure,
                           @JsonProperty("name") String name,
                           @JsonProperty("email") String email) {
        this.reservationId = reservationId;
        this.arrival = arrival;
        this.departure = departure;
        this.name = name;
        this.email = email;
    }
}
