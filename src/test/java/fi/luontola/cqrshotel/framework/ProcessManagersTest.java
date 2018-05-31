// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Category(FastTests.class)
public class ProcessManagersTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final UUID registerId = UUID.randomUUID();
    private static final UUID registerId2 = UUID.randomUUID();

    private final SpyMessageGateway gateway = new SpyMessageGateway();
    private final ProcessRepo repo = new ProcessRepo();
    private final ProcessManagers processManagers = new ProcessManagers(repo, gateway)
            .register(RegisterProcess.class, RegisterProcess::entryPoint);

    // TODO: duplicate events are ignored
    // TODO: removing completed processes
    // TODO: error handling: one crashing process should prevent others from progressing

    @Test
    public void processes_receive_events_and_publish_commands() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 42)));

        assertThat(gateway.outgoingMessages(), is(Arrays.asList(
                new ShowCurrentValue(registerId, 42))));
    }

    @Test
    public void published_commands_have_the_event_ID_as_their_causation_ID() {
        Envelope<Event> event = Envelope.newMessage(new RegisterCreated(registerId, 42));
        processManagers.handle(event);

        assertThat(gateway.latestMessage().causationId, is(event.messageId));
    }

    @Test
    public void published_commands_have_the_process_ID_as_their_correlation_ID() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 42)));

        // XXX: we cannot directly access the process ID, but we can look for a process with the same ID as this correlation ID
        UUID processId = gateway.latestMessage().correlationId;
        PersistedProcess process = repo.getById(processId);
        assertThat(process, is(notNullValue()));
    }

    @Test
    public void processes_are_stateful() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20), gateway.latestMessage()));

        assertThat(gateway.outgoingMessages(), is(Arrays.asList(
                new ShowCurrentValue(registerId, 10),
                new ShowCurrentValue(registerId, 30))));
    }

    @Test
    public void process_state_is_independent_from_other_processes() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        Envelope<?> command1 = gateway.latestMessage();
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId2, 100)));
        Envelope<?> command2 = gateway.latestMessage();
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20), command1));
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId2, 200), command2));

        assertThat(gateway.outgoingMessages(), is(Arrays.asList(
                new ShowCurrentValue(registerId, 10),
                new ShowCurrentValue(registerId2, 100),
                new ShowCurrentValue(registerId, 30),
                new ShowCurrentValue(registerId2, 300))));
    }

    @Test
    public void when_loading_an_existing_process_the_commands_from_old_events_are_not_republished() {
        processManagers.handle(Envelope.newMessage(new RegisterCreated(registerId, 10)));
        SpyMessageGateway gateway2 = new SpyMessageGateway();
        ProcessManagers processManagers2 = new ProcessManagers(repo, gateway2)
                .register(RegisterProcess.class, RegisterProcess::entryPoint);

        processManagers2.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 20), gateway.latestMessage()));

        assertThat(gateway2.outgoingMessages(), is(Arrays.asList(new ShowCurrentValue(registerId, 30))));
    }

    @Test
    public void ignores_events_which_nobody_is_subscribed_to() {
        processManagers.handle(Envelope.newMessage(new ValueAddedToRegister(registerId, 42)));

        assertThat(gateway.outgoingMessages(), is(empty()));
    }

    @Test
    public void cannot_register_the_same_process_twice() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(is("Process already registered: class fi.luontola.cqrshotel.framework.ProcessManagersTest$RegisterProcess"));
        processManagers.register(RegisterProcess.class, RegisterProcess::entryPoint);
    }


    // guinea pigs

    static class RegisterProcess extends AnnotatedProjection {
        private final Publisher publisher;
        private int value;

        public RegisterProcess(Publisher publisher) {
            this.publisher = publisher;
        }

        public static boolean entryPoint(Event event) {
            return event instanceof RegisterCreated;
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

    private final ProcessRepo processRepo;
    private final MessageGateway gateway;
    private final List<EntryPoint> entryPoints = new ArrayList<>();

    public ProcessManagers(ProcessRepo processRepo, MessageGateway gateway) {
        this.processRepo = processRepo;
        this.gateway = gateway;
    }

    public ProcessManagers register(Class<? extends Projection> processType, Predicate<Event> entryPoint) {
        if (entryPoints.stream().anyMatch(registered -> registered.processType.equals(processType))) {
            throw new IllegalArgumentException("Process already registered: " + processType);
        }
        entryPoints.add(new EntryPoint(processType, entryPoint));
        return this;
    }

    public void handle(Envelope<Event> event) {
        startNewProcesses(event);
        for (ProcessManager process : findSubscribedProcesses(event)) {
            process.handle(event);
        }
    }

    // startup

    private void startNewProcesses(Envelope<Event> event) {
        for (EntryPoint entryPoint : entryPoints) {
            if (entryPoint.matches(event)) {
                startNewProcess(entryPoint.processType, event);
            }
        }
    }

    private void startNewProcess(Class<? extends Projection> processType, Envelope<Event> initialEvent) {
        UUID processId = UUIDs.newUUID();
        processRepo.create(processId, processType);
        ProcessManager pm = new ProcessManager(processId, processRepo, gateway);
        pm.subscribe(processId); // subscribe to itself as correlationId to receive responses to own commands
        pm.subscribe(initialEvent.messageId); // always handle the first message (it won't have processId as correlationId)
    }

    // lookup

    private List<ProcessManager> findSubscribedProcesses(Envelope<Event> event) {
        List<UUID> topics = getTopics(event);
        Set<UUID> processIds = processRepo.findSubscribersToAnyOf(topics);
        return processIds.stream()
                .map(processId -> new ProcessManager(processId, processRepo, gateway))
                .collect(Collectors.toList());
    }

    private static List<UUID> getTopics(Envelope<Event> event) {
        List<UUID> topics = UUIDs.extractUUIDs(event.payload);
        topics.add(event.correlationId);
        topics.add(event.messageId);
        return topics;
    }


    private static class EntryPoint {
        final Class<? extends Projection> processType;
        private final Predicate<Event> entryPoint;

        public EntryPoint(Class<? extends Projection> processType, Predicate<Event> entryPoint) {
            this.processType = processType;
            this.entryPoint = entryPoint;
        }

        public boolean matches(Envelope<Event> event) {
            return entryPoint.test(event.payload);
        }
    }
}

class ProcessManager { // TODO: merge ProcessManager and PersistedProcess?

    private final UUID processId;
    private final ProcessRepo processRepo;
    private final MessageGateway gateway;

    public ProcessManager(UUID processId, ProcessRepo processRepo, MessageGateway gateway) {
        this.processId = processId;
        this.processRepo = processRepo;
        this.gateway = gateway;
    }

    public void subscribe(UUID id) {
        processRepo.subscribe(processId, id);
    }

    public void handle(Envelope<Event> event) {
        SpyPublisher publisher = new SpyPublisher();
        PersistedProcess state = processRepo.getById(processId);
        Projection process = state.newInstance(publisher);
        state.history.forEach(e -> process.apply(e.payload));
        publisher.publishedMessages.clear();

        process.apply(event.payload);

        processRepo.save(processId, event);
        publisher.publishedMessages.stream()
                .map(m -> Envelope.newMessage(m, event).withCorrelationId(processId))
                .forEach(gateway::send);
    }
}

class ProcessRepo {

    private final Map<UUID, PersistedProcess> processesById = new HashMap<>();
    private final Multimap<UUID, UUID> subscribedProcessesByTopic = ArrayListMultimap.create(16, 1);

    public void create(UUID processId, Class<?> processType) {
        processesById.put(processId, new PersistedProcess(processType));
    }

    public PersistedProcess getById(UUID processId) {
        return processesById.computeIfAbsent(processId, key -> {
            throw new IllegalArgumentException("Process not found: " + key);
        });
    }

    public void save(UUID processId, Envelope<Event> processedEvent) {
        PersistedProcess process = getById(processId);
        process.history.add(processedEvent);
    }

    public void subscribe(UUID processId, UUID topic) {
        subscribedProcessesByTopic.put(topic, processId);
    }

    public Set<UUID> findSubscribersToAnyOf(List<UUID> topics) {
        Set<UUID> processIds = new HashSet<>();
        for (UUID topic : topics) {
            processIds.addAll(subscribedProcessesByTopic.get(topic));
        }
        return processIds;
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
