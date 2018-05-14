// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.util.Struct;

import java.time.Instant;

public class RoomAvailabilityIntervalDto extends Struct {

    public final Instant start;
    public final Instant end;
    public final boolean occupied;

    @JsonCreator
    public RoomAvailabilityIntervalDto(@JsonProperty("start") Instant start,
                                       @JsonProperty("end") Instant end,
                                       @JsonProperty("occupied") boolean occupied) {
        this.start = start;
        this.end = end;
        this.occupied = occupied;
    }

    public boolean overlapsWith(Instant start, Instant end) {
        return this.start.isBefore(end) && this.end.isAfter(start);
    }
}
