// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static fi.luontola.cqrshotel.framework.EventListeners.Requirements.MUST_BE_PRIVATE;

public abstract class AggregateRoot {

    private final EventListeners eventListeners;
    private final List<Event> changes = new ArrayList<>();
    private UUID id;
    private int version = 0;

    public AggregateRoot() {
        eventListeners = EventListeners.of(this, MUST_BE_PRIVATE);
    }

    public final UUID getId() {
        if (id == null) {
            throw new IllegalStateException("id not set");
        }
        return id;
    }

    final void setId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("id already set");
        }
        this.id = id;
    }

    public final int getVersion() {
        return version;
    }

    public final List<Event> getUncommittedChanges() {
        return Collections.unmodifiableList(changes);
    }

    public final void markChangesAsCommitted() {
        changes.clear();
    }

    public final void loadFromHistory(Iterable<Event> history) {
        for (Event event : history) {
            applyChange(event, false);
        }
    }

    protected final void publish(Event event) {
        applyChange(event, true);
    }

    private void applyChange(Event event, boolean isNew) {
        eventListeners.send(event);
        version++;
        if (isNew) {
            changes.add(event);
        }
    }
}
