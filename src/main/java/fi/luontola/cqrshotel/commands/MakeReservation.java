// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.util.Struct;

import java.time.LocalDate;

public class MakeReservation extends Struct implements Command {

    public final LocalDate startDate;
    public final LocalDate endDate;
    public final String name;
    public final String email;

    @JsonCreator
    public MakeReservation(@JsonProperty("startDate") LocalDate startDate,
                           @JsonProperty("endDate") LocalDate endDate,
                           @JsonProperty("name") String name,
                           @JsonProperty("email") String email) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.email = email;
    }
}
