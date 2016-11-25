// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    int BEGINNING = 0;

    long saveEvents(UUID streamId, List<Event> newEvents, int expectedVersion);

    default List<Event> getEventsForStream(UUID streamId) {
        return getEventsForStream(streamId, BEGINNING);
    }

    List<Event> getEventsForStream(UUID streamId, int sinceVersion);

    default List<Event> getAllEvents() {
        return getAllEvents(BEGINNING);
    }

    List<Event> getAllEvents(long sincePosition);

    int getCurrentVersion(UUID streamId);

    long getCurrentPosition();
}
