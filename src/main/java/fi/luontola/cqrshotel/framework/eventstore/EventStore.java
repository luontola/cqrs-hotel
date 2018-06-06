// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    int BEGINNING = 0;

    long saveEvents(UUID streamId, List<Envelope<Event>> newEvents, int expectedVersion);

    default List<PersistedEvent> getEventsForStream(UUID streamId) {
        return getEventsForStream(streamId, BEGINNING);
    }

    List<PersistedEvent> getEventsForStream(UUID streamId, int sinceVersion);

    default List<PersistedEvent> getAllEvents() {
        return getAllEvents(BEGINNING);
    }

    List<PersistedEvent> getAllEvents(long sincePosition);

    int getCurrentVersion(UUID streamId);

    long getCurrentPosition();
}
