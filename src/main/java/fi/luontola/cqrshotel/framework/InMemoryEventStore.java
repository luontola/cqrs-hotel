// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class InMemoryEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final ConcurrentMap<UUID, List<Event>> streamsById = new ConcurrentHashMap<>();
    private final List<Event> allEvents = new ArrayList<>();

    @Override
    public long saveEvents(UUID streamId, List<Event> newEvents, int expectedVersion) {
        List<Event> events = streamsById.computeIfAbsent(streamId, uuid -> new ArrayList<>());
        synchronized (events) {
            int actualVersion = events.size();
            if (expectedVersion != actualVersion) {
                throw new OptimisticLockingException("expected version " + expectedVersion + " but was " + actualVersion + " for stream " + streamId);
            }
            for (Event newEvent : newEvents) {
                events.add(newEvent);
                int newVersion = events.size();
                log.info("Saved stream {} version {}: {}", streamId, newVersion, newEvent);
            }
            synchronized (allEvents) {
                allEvents.addAll(newEvents);
                return allEvents.size();
            }
        }
    }

    @Override
    public List<Event> getEventsForStream(UUID streamId, int sinceVersion) {
        List<Event> events = streamsById.get(streamId);
        if (events == null) {
            throw new EventStreamNotFoundException(streamId);
        }
        synchronized (events) {
            return events.stream()
                    .skip(sinceVersion)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Event> getAllEvents(long sincePosition) {
        synchronized (allEvents) {
            return allEvents.stream()
                    .skip(sincePosition)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public int getCurrentVersion(UUID streamId) {
        List<Event> events = streamsById.get(streamId);
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
