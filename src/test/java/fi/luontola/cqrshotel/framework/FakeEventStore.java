// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class FakeEventStore implements EventStore {

    public List<Event> existing = Collections.emptyList();
    public final List<Event> produced = new ArrayList<>();
    private UUID expectedStreamId;

    @Override
    public List<Event> getEventsForStream(UUID streamId) {
        assertThat("streamId", streamId, is(notNullValue()));
        if (expectedStreamId == null) {
            expectedStreamId = streamId;
        }
        assertThat("streamId", streamId, is(expectedStreamId));
        return existing;
    }

    @Override
    public void saveEvents(UUID streamId, List<Event> newEvents, int expectedVersion) {
        assertThat("streamId", streamId, is(expectedStreamId));
        produced.addAll(newEvents);
    }
}
