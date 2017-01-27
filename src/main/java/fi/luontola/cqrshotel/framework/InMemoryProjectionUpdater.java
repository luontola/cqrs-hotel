// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.List;

public class InMemoryProjectionUpdater {

    private final EventListeners projection;
    private final EventStore eventStore;
    private long position = EventStore.BEGINNING;

    public InMemoryProjectionUpdater(Projection projection, EventStore eventStore) {
        this.projection = EventListeners.of(projection);
        this.eventStore = eventStore;
    }

    public void update() {
        List<Event> events = eventStore.getAllEvents(position);
        for (Event event : events) {
            projection.send(event);
            position++;
        }
    }
}
