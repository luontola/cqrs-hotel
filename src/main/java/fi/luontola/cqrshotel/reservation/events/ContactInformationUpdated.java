// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.Struct;

import java.util.UUID;

public class ContactInformationUpdated extends Struct implements Event {

    public final UUID reservationId;
    public final String name;
    public final String email;

    @JsonCreator
    public ContactInformationUpdated(@JsonProperty("reservationId") UUID reservationId,
                                     @JsonProperty("name") String name,
                                     @JsonProperty("email") String email) {
        this.reservationId = reservationId;
        this.name = name;
        this.email = email;
    }
}
