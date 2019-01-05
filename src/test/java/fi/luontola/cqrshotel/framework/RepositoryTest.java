// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.eventstore.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.eventstore.OptimisticLockingException;
import fi.luontola.cqrshotel.framework.util.EventListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class RepositoryTest {

    private final GuineaPigRepository repo = new GuineaPigRepository(new InMemoryEventStore());
    private final UUID id = UUID.randomUUID();

    @Test
    public void can_create_new_entity() {
        var entity = repo.create(id);

        assertThat(entity.getVersion(), is(0));
    }

    @Test
    public void cannot_overwrite_existing_entity() {
        saveEvents(id, "foo");

        var entity = repo.create(id);
        var e = assertThrows(OptimisticLockingException.class, () -> {
            repo.save(entity, entity.getVersion());
        });
        assertThat(e.getMessage(), containsString("expected version 0 but was 1"));
    }

    @Test
    public void can_get_existing_entity_by_id() {
        saveEvents(id, "foo");

        var entity = repo.getById(id);

        assertThat(entity.getVersion(), is(1));
        assertThat(entity.value, is("foo"));
    }

    @Test
    public void cannot_get_non_existing_entity_by_id() {
        var e = assertThrows(EntityNotFoundException.class, () -> {
            repo.getById(id);
        });
        assertThat(e.getMessage(), is(id.toString()));
    }

    @Test
    public void lazy_create_can_return_new_entity() {
        var entity = repo.createOrGet(id);

        assertThat(entity.getVersion(), is(0));
    }

    @Test
    public void lazy_create_can_return_existing_entity() {
        saveEvents(id, "foo");

        var entity = repo.createOrGet(id);

        assertThat(entity.getVersion(), is(1));
        assertThat(entity.value, is("foo"));
    }

    @Test
    public void save_returns_the_global_committed_position() {
        var commit1 = saveEvents(UUID.randomUUID(), "event1", "event2", "event3");
        assertThat(commit1.committedPosition, is(3L));

        var commit2 = saveEvents(UUID.randomUUID(), "event4", "event5");
        assertThat(commit2.committedPosition, is(5L));
    }

    private Commit saveEvents(UUID id, String... values) {
        var entity = repo.createOrGet(id);
        var originalVersion = entity.getVersion();
        for (var value : values) {
            entity.setValue(value);
        }
        return repo.save(entity, originalVersion);
    }

    static class GuineaPigRepository extends Repository<GuineaPig> {
        public GuineaPigRepository(EventStore eventStore) {
            super(eventStore);
        }
    }

    static class GuineaPig extends AggregateRoot {
        public String value;

        @EventListener
        private void apply(ValueChanged event) {
            this.value = event.value;
        }

        public void setValue(String value) {
            publish(new ValueChanged(value));
        }
    }

    static class ValueChanged implements Event {
        public final String value;

        public ValueChanged(String value) {
            this.value = value;
        }
    }
}
