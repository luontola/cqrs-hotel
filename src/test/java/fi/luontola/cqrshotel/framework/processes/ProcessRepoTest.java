// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.framework.eventstore.OptimisticLockingException;
import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.UUID;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ProcessRepoTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final UUID processId = UUID.randomUUID();
    private static final UUID topic = UUID.randomUUID();

    private final ProcessRepo repo = new ProcessRepo();

    @Test
    public void creates_new_processes() {
        var newProcess = repo.create(processId, DummyProcess.class);
        repo.save(newProcess);

        var savedProcess = repo.getById(processId);
        assertThat("processId", savedProcess.processId, is(processId));
        assertThat("processType", savedProcess.processType, is(equalTo(DummyProcess.class)));
    }

    @Test
    public void cannot_create_multiple_processes_with_same_ID() {
        var p1 = repo.create(processId, DummyProcess.class);
        var p2 = repo.create(processId, DummyProcess.class);
        repo.save(p1);

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage(is("expected version 0 but was 1 for process " + processId));
        repo.save(p2);
    }

    @Test
    public void cannot_modify_the_same_process_concurrently() {
        createProcess();

        var p1 = repo.getById(processId);
        var p2 = repo.getById(processId);
        p1.subscribe(topic);
        p2.subscribe(topic);
        repo.save(p1);

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage("expected version 1 but was 2 for process " + processId);
        repo.save(p2);
    }

    @Test
    public void processes_can_subscribe_to_topics() {
        createProcess(p -> p.subscribe(topic));

        assertThat(repo.findSubscribersToAnyOf(topic), contains(processId));
    }

    @Test
    public void processes_can_unsubscribe_from_topics() {
        createProcess(p -> p.subscribe(topic));

        updateProcess(p -> p.unsubscribe(topic));

        assertThat(repo.findSubscribersToAnyOf(topic), is(empty()));
    }

    @Test
    public void subscribing_and_unsubscribing_is_idempotent() {
        createProcess();
        assertThat("initial", repo.findSubscribersToAnyOf(topic), is(empty()));

        updateProcess(p -> p.subscribe(topic));
        assertThat("1st subscribe", repo.findSubscribersToAnyOf(topic), contains(processId));

        updateProcess(p -> p.subscribe(topic));
        assertThat("2nd subscribe", repo.findSubscribersToAnyOf(topic), contains(processId));

        updateProcess(p -> p.unsubscribe(topic));
        assertThat("1st unsubscribe", repo.findSubscribersToAnyOf(topic), is(empty()));

        updateProcess(p -> p.unsubscribe(topic));
        assertThat("2nd unsubscribe", repo.findSubscribersToAnyOf(topic), is(empty()));
    }


    // helpers

    private void createProcess() {
        createProcess(process -> {
        });
    }

    private void createProcess(Consumer<ProcessManager> action) {
        var process = repo.create(processId, DummyProcess.class);
        action.accept(process);
        repo.save(process);
    }

    private void updateProcess(Consumer<ProcessManager> action) {
        var process = repo.getById(processId);
        action.accept(process);
        repo.save(process);
    }


    // guinea pigs

    private static class DummyProcess extends AnnotatedProjection {
        public DummyProcess(Publisher publisher) {
        }
    }
}
