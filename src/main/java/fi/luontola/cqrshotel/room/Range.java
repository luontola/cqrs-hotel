// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.util.Struct;

import java.time.Instant;

public class Range extends Struct {

    public final Instant start;
    public final Instant end;

    public Range(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end, but was: start " + start + ", end " + end);
        }
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(Range that) {
        return this.start.isBefore(that.end) &&
                this.end.isAfter(that.start);
    }
}
