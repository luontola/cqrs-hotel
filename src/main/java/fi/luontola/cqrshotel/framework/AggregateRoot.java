// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static fi.luontola.cqrshotel.framework.EventListeners.Requirements.MUST_BE_PRIVATE;

/**
 * Aggregate root is the consistency boundary for doing atomic transactions.
 * You may obtain {@code AggregateRoot} instances from a {@link Repository}.
 * <p>
 * The methods of an {@code AggregateRoot} implementation MUST follow a strong dichotomy of
 * event listeners and command handlers:
 * <p>
 * <b>Event listeners</b> are private methods, annotated with {@link EventListener},
 * which take an {@link Event} as parameter. Their purpose is to read data from previously published events.
 * The framework will call them automatically for all old and newly published events of this aggregate.
 * <em>Event listeners MAY mutate state and MUST NOT throw exceptions.</em>
 * <p>
 * <b>Command handlers</b> are public methods which take arbitrary parameters and are called by users.
 * Their purpose is to validate business rules and publish new events using {@link #publish(Event)}.
 * The published events will be persisted atomically by {@link Repository#save}.
 * <em>Command handlers MUST NOT mutate state and MAY throw exceptions.</em>
 */
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
            applyChange(event);
        }
    }

    protected final void publish(Event event) {
        applyChange(event);
        changes.add(event);
    }

    private void applyChange(Event event) {
        eventListeners.send(event);
        version++;
    }
}
