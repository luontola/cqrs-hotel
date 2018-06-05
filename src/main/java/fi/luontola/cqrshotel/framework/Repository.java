// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.framework.eventstore.EventStore;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository is the way for creating and modifying aggregate roots.
 */
public class Repository<T extends AggregateRoot> {

    private final Class<T> aggregateType;
    private final EventStore eventStore;

    public Repository(EventStore eventStore) {
        this.aggregateType = getAggregateRootType();
        this.eventStore = eventStore;
    }

    @SuppressWarnings("unchecked")
    private Class<T> getAggregateRootType() {
        ParameterizedType myType = (ParameterizedType) getClass().getGenericSuperclass();
        Class<?> typeArg = (Class<?>) myType.getActualTypeArguments()[0];
        if (AggregateRoot.class.isAssignableFrom(typeArg)) {
            return (Class<T>) typeArg;
        } else {
            throw new IllegalArgumentException("Not aggregate root: " + typeArg);
        }
    }

    public T create(UUID id) {
        try {
            T aggregate = aggregateType.newInstance();
            aggregate.setId(id);
            return aggregate;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public T createOrGet(UUID id) {
        T aggregate = create(id);
        List<Envelope<Event>> events = eventStore.getEventsForStream(id);
        aggregate.loadFromHistory(
                events.stream()
                        .map(e -> e.payload)
                        .collect(Collectors.toList()));
        return aggregate;
    }

    public T getById(UUID id) {
        T aggregate = createOrGet(id);
        if (aggregate.getVersion() == EventStore.BEGINNING) {
            throw new EntityNotFoundException(id);
        }
        return aggregate;
    }

    public Commit save(T aggregate, int expectedVersion) {
        List<Envelope<Event>> events = aggregate.getUncommittedChanges().stream()
                .map(Envelope::newMessage)
                .collect(Collectors.toList());
        long committedPosition = eventStore.saveEvents(aggregate.getId(), events, expectedVersion);
        aggregate.markChangesAsCommitted();
        return new Commit(committedPosition);
    }
}
