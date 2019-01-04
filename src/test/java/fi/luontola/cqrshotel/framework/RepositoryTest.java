// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.eventstore.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.eventstore.OptimisticLockingException;
import fi.luontola.cqrshotel.framework.util.EventListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class RepositoryTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage("expected version 0 but was 1");
        var entity = repo.create(id);
        repo.save(entity, entity.getVersion());
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
        thrown.expect(EntityNotFoundException.class);
        thrown.expectMessage(id.toString());
        repo.getById(id);
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
