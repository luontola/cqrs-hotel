// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.List;
import java.util.UUID;

public abstract class StreamProjection {

    private final UUID streamId;
    private final EventListeners eventListeners;
    private final EventStore eventStore;
    private int version = EventStore.BEGINNING;

    public StreamProjection(UUID streamId, EventStore eventStore) {
        this.streamId = streamId;
        this.eventListeners = EventListeners.of(this);
        this.eventStore = eventStore;
    }

    public final void update() {
        List<Event> events = eventStore.getEventsForStream(streamId, version);
        for (Event event : events) {
            eventListeners.send(event);
            version++;
        }
    }
}
