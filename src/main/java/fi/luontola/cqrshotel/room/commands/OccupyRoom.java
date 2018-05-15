// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.util.Struct;

import java.time.Instant;
import java.util.UUID;

public class OccupyRoom extends Struct implements Command {

    public final UUID roomId;
    public final Instant start;
    public final Instant end;
    public final UUID occupant;

    @JsonCreator
    public OccupyRoom(@JsonProperty("roomId") UUID roomId,
                      @JsonProperty("start") Instant start,
                      @JsonProperty("end") Instant end,
                      @JsonProperty("occupant") UUID occupant) {
        this.roomId = roomId;
        this.start = start;
        this.end = end;
        this.occupant = occupant;
    }
}
