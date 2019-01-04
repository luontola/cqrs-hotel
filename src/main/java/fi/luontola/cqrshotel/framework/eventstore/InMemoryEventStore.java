// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final Map<UUID, List<PersistedEvent>> streamsById = new HashMap<>();
    private final List<PersistedEvent> allEvents = new ArrayList<>();

    @Override
    public synchronized long saveEvents(UUID streamId, List<Envelope<Event>> newEvents, int expectedVersion) {
        var stream = streamsById.computeIfAbsent(streamId, uuid -> new ArrayList<>());
        var actualVersion = stream.size();
        if (expectedVersion != actualVersion) {
            throw new OptimisticLockingException("expected version " + expectedVersion + " but was " + actualVersion + " for stream " + streamId);
        }
        for (var newEvent : newEvents) {
            var persisted = new PersistedEvent(newEvent, streamId, stream.size() + 1, allEvents.size() + 1);
            stream.add(persisted);
            allEvents.add(persisted);
            log.trace("Saved stream {} version {}: {}", persisted.streamId, persisted.version, persisted.event);
        }
        return allEvents.size();
    }

    @Override
    public synchronized List<PersistedEvent> getEventsForStream(UUID streamId, int sinceVersion) {
        var stream = streamsById.getOrDefault(streamId, Collections.emptyList());
        return readSince(sinceVersion, stream);
    }

    @Override
    public synchronized List<PersistedEvent> getAllEvents(long sincePosition) {
        return readSince(sincePosition, allEvents);
    }

    private static ArrayList<PersistedEvent> readSince(long sincePosition, List<PersistedEvent> events) {
        return new ArrayList<>(events.subList((int) sincePosition, events.size()));
    }

    @Override
    public synchronized int getCurrentVersion(UUID streamId) {
        var events = streamsById.get(streamId);
        if (events == null) {
            return BEGINNING;
        }
        return events.size();
    }

    @Override
    public synchronized long getCurrentPosition() {
        return allEvents.size();
    }
}
