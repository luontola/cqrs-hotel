// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.projections;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.EventListeners;

public abstract class AnnotatedProjection implements Projection {

    private final EventListeners eventListeners = EventListeners.of(this);

    public void apply(Envelope<Event> event) {
        eventListeners.send(event.payload);
    }
}
