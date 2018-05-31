// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.framework.SpyMessageGateway;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.UUID;

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
        thrown.expectMessage(is("Process already registered: class fi.luontola.cqrshotel.framework.processes.ProcessManagersTest$RegisterProcess"));
        processManagers.register(RegisterProcess.class, RegisterProcess::entryPoint);
    }


    // guinea pigs

    public static class RegisterProcess extends AnnotatedProjection {
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

    public static class RegisterCreated extends Struct implements Event {
        public final UUID registerId;
        public final int initialValue;

        public RegisterCreated(UUID registerId, int initialValue) {
            this.registerId = registerId;
            this.initialValue = initialValue;
        }
    }

    public static class ValueAddedToRegister extends Struct implements Event {
        public final UUID registerId;
        public final int value;

        public ValueAddedToRegister(UUID registerId, int value) {
            this.registerId = registerId;
            this.value = value;
        }
    }

    public static class ShowCurrentValue extends Struct implements Command {
        public final UUID registerId;
        public final int value;

        public ShowCurrentValue(UUID registerId, int value) {
            this.registerId = registerId;
            this.value = value;
        }
    }
}
