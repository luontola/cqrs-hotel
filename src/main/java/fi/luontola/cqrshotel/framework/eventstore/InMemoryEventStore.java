// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final ConcurrentMap<UUID, List<Envelope<Event>>> streamsById = new ConcurrentHashMap<>();
    private final List<Envelope<Event>> allEvents = new ArrayList<>();

    @Override
    public long saveEvents(UUID streamId, List<Envelope<Event>> newEvents, int expectedVersion) {
        List<Envelope<Event>> events = streamsById.computeIfAbsent(streamId, uuid -> new ArrayList<>());
        synchronized (events) {
            int actualVersion = events.size();
            if (expectedVersion != actualVersion) {
                throw new OptimisticLockingException("expected version " + expectedVersion + " but was " + actualVersion + " for stream " + streamId);
            }
            for (Envelope<Event> newEvent : newEvents) {
                events.add(newEvent);
                int newVersion = events.size();
                log.trace("Saved stream {} version {}: {}", streamId, newVersion, newEvent);
            }
            synchronized (allEvents) {
                allEvents.addAll(newEvents);
                return allEvents.size();
            }
        }
    }

    @Override
    public List<Envelope<Event>> getEventsForStream(UUID streamId, int sinceVersion) {
        List<Envelope<Event>> events = streamsById.getOrDefault(streamId, Collections.emptyList());
        synchronized (events) {
            return readSince(sinceVersion, events);
        }
    }

    @Override
    public List<Envelope<Event>> getAllEvents(long sincePosition) {
        synchronized (allEvents) {
            return readSince(sincePosition, allEvents);
        }
    }

    private static ArrayList<Envelope<Event>> readSince(long sincePosition, List<Envelope<Event>> events) {
        return new ArrayList<>(events.subList((int) sincePosition, events.size()));
    }

    @Override
    public int getCurrentVersion(UUID streamId) {
        List<Envelope<Event>> events = streamsById.get(streamId);
        if (events == null) {
            return BEGINNING;
        }
        synchronized (events) {
            return events.size();
        }
    }

    @Override
    public long getCurrentPosition() {
        synchronized (allEvents) {
            return allEvents.size();
        }
    }
}
