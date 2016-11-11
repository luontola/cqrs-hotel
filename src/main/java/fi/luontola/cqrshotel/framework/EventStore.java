// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventStore {

    public static final int NO_EXISTING_VERSIONS = 0;

    private final ConcurrentMap<UUID, List<Event>> aggregates = new ConcurrentHashMap<>();

    public List<Event> getEventsForAggregate(UUID id) {
        List<Event> events = aggregates.get(id);
        if (events == null) {
            throw new AggregateNotFoundException(id);
        }
        return new ArrayList<>(events);
    }

    public void saveEvents(UUID id, List<Event> newEvents, int expectedVersion) {
        List<Event> events = aggregates.computeIfAbsent(id, uuid -> new ArrayList<>());
        synchronized (events) {
            int actualVersion = events.size();
            if (expectedVersion != actualVersion) {
                throw new OptimisticLockingException("expected version " + expectedVersion + " but was " + actualVersion + " for " + id);
            }
            events.addAll(newEvents);
        }
    }
}
