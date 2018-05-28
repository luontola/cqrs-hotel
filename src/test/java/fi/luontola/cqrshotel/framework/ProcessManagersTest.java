// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.ProcessManagersTest.RegisterCreated;
import fi.luontola.cqrshotel.framework.ProcessManagersTest.RegisterProcess;
import fi.luontola.cqrshotel.framework.ProcessManagersTest.ValueAddedToRegister;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ProcessManagersTest {

    private static final UUID registerId = UUID.randomUUID();
    private static final UUID registerId2 = UUID.randomUUID();

    private final SpyPublisher publisher = new SpyPublisher();
    private final ProcessRepo repo = new ProcessRepo();
    private final ProcessManagers processManagers = new ProcessManagers(repo, publisher);

    // TODO: duplicate events are ignored
    // TODO: sends_subscribed_events_to_the_same_process
    // TODO: ignores events which nobody has subscribed

    @Test
    public void processes_receive_events_and_publish_commands() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 42)));

        assertThat(publisher.publishedMessages, is(Arrays.asList(
                new ShowCurrentValue(registerId, 42))));
    }

    @Test
    public void processes_are_stateful() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20)));

        assertThat(publisher.publishedMessages, is(Arrays.asList(
                new ShowCurrentValue(registerId, 10),
                new ShowCurrentValue(registerId, 30))));
    }

    @Test
    public void process_state_is_independent_from_other_processes() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId2, 100)));
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20)));
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId2, 200)));

        assertThat(publisher.publishedMessages, is(Arrays.asList(
                new ShowCurrentValue(registerId, 10),
                new ShowCurrentValue(registerId2, 100),
                new ShowCurrentValue(registerId, 30),
                new ShowCurrentValue(registerId2, 300))));
    }

    @Test
    public void when_loading_an_existing_process_the_commands_from_old_events_are_not_republished() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        SpyPublisher publisher2 = new SpyPublisher();
        ProcessManagers processManagers2 = new ProcessManagers(repo, publisher2);

        processManagers2.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20)));

        assertThat(publisher2.publishedMessages, is(Arrays.asList(new ShowCurrentValue(registerId, 30))));
    }


    // guinea pigs

    static class RegisterProcess extends AnnotatedProjection {
        private final Publisher publisher;
        private int value;

        public RegisterProcess(Publisher publisher) {
            this.publisher = publisher;
        }

        @EventListener
        public void handle(RegisterCreated event) {
            value = event.initialValue;
            publisher.publish(new ShowCurrentValue(event.registerId, value));
        }

        @EventListener
        public void handle(ValueAddedToRegister event) {
            value += event.value;
            publisher.publish(new ShowCurrentValue(event.registerId, value));
        }
    }

    static class RegisterCreated extends Struct implements Event {
        public final UUID registerId;
        public final int initialValue;

        public RegisterCreated(UUID registerId, int initialValue) {
            this.registerId = registerId;
            this.initialValue = initialValue;
        }
    }

    static class ValueAddedToRegister extends Struct implements Event {
        public final UUID registerId;
        public final int value;

        public ValueAddedToRegister(UUID registerId, int value) {
            this.registerId = registerId;
            this.value = value;
        }
    }

    static class ShowCurrentValue extends Struct implements Command {
        public final UUID registerId;
        public final int value;

        public ShowCurrentValue(UUID registerId, int value) {
            this.registerId = registerId;
            this.value = value;
        }
    }
}

// SUT

class ProcessManagers {

    private final ProcessRepo processes;
    private final Publisher realPublisher;

    public ProcessManagers(ProcessRepo processes, Publisher publisher) {
        this.processes = processes;
        this.realPublisher = publisher;
    }

    public void handle(Envelope<Event> event) {
        startNewProcesses(event, processes);
        UUID processId = findProcessForHanding(event);
        if (processId != null) {
            delegateEventToProcess(event, processId);
        }
    }

    private static void startNewProcesses(Envelope<Event> event, ProcessRepo processes) {
        // TODO: decouple from the guinea pigs
        // TODO: introduce subscriptions to decouple process ID from entity IDs
        if (event.payload instanceof RegisterCreated) {
            UUID processId = ((RegisterCreated) event.payload).registerId;
            processes.create(processId, RegisterProcess.class);
        }
    }

    private static UUID findProcessForHanding(Envelope<Event> event) {
        // TODO: decouple from the guinea pigs
        if (event.payload instanceof RegisterCreated) {
            return ((RegisterCreated) event.payload).registerId;
        }
        if (event.payload instanceof ValueAddedToRegister) {
            return ((ValueAddedToRegister) event.payload).registerId;
        }
        return null;
    }

    private void delegateEventToProcess(Envelope<Event> event, UUID processId) {
        SpyPublisher publisher = new SpyPublisher();
        PersistedProcess state = processes.getById(processId);
        Projection process = state.newInstance(publisher);
        state.history.forEach(e -> process.apply(e.payload));
        publisher.publishedMessages.clear();

        process.apply(event.payload);

        processes.save(processId, event);
        publisher.publishedMessages.forEach(realPublisher::publish);
    }
}

class ProcessRepo {

    private final Map<UUID, PersistedProcess> processesById = new HashMap<>();

    public void create(UUID processId, Class<?> processType) {
        processesById.put(processId, new PersistedProcess(processType));
    }

    public PersistedProcess getById(UUID processId) {
        return processesById.get(processId);
    }

    public void save(UUID processId, Envelope<Event> processedEvent) {
        PersistedProcess process = getById(processId);
        process.history.add(processedEvent);
    }
}

class PersistedProcess {

    private final Class<?> processType;
    public final List<Envelope<Event>> history = new ArrayList<>();

    public PersistedProcess(Class<?> processType) {
        this.processType = processType;
    }

    public Projection newInstance(Publisher publisher) {
        try {
            return (Projection) processType.getConstructor(Publisher.class).newInstance(publisher);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate " + processType, e);
        }
    }
}
