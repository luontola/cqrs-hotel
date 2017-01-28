// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.List;
import java.util.UUID;

public class InMemorySingleStreamProjectionUpdater {

    private final UUID streamId;
    private final EventListeners projection;
    private final EventStore eventStore;
    private int version = EventStore.BEGINNING;

    public InMemorySingleStreamProjectionUpdater(UUID streamId, SingleStreamProjection projection, EventStore eventStore) {
        this.streamId = streamId;
        this.projection = EventListeners.of(projection);
        this.eventStore = eventStore;
    }

    public void update() {
        List<Event> events = eventStore.getEventsForStream(streamId, version);
        for (Event event : events) {
            projection.send(event);
            version++;
        }
    }
}
