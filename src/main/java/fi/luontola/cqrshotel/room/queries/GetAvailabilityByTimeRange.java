// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Query;
import fi.luontola.cqrshotel.framework.util.Struct;

import java.time.Instant;

public class GetAvailabilityByTimeRange extends Struct implements Query {

    public final Instant start;
    public final Instant end;

    @JsonCreator
    public GetAvailabilityByTimeRange(@JsonProperty("start") Instant start,
                                      @JsonProperty("end") Instant end) {
        this.start = start;
        this.end = end;
    }
}
