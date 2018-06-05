// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.projections;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Query;

/**
 * Projections are the way for reading and analyzing event-sourced data.
 * They will be called for every {@link Event event} in the system.
 * This way projections can construct a read-optimized model from the event data
 * for the purpose of serving {@link Query queries} efficiently.
 */
public interface Projection {

    void apply(Envelope<Event> event);

    default String getProjectionName() {
        return getClass().getSimpleName();
    }
}
