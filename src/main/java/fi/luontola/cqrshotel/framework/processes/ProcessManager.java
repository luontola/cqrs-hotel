// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.framework.MessageGateway;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.framework.processes.events.ProcessStarted;
import fi.luontola.cqrshotel.framework.processes.events.ProcessSubscribedToTopic;
import fi.luontola.cqrshotel.framework.processes.events.ProcessUnsubscribedFromTopic;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProcessManager {

    public final UUID processId;
    public final Class<?> processType;
    private final List<Envelope<Event>> history = new ArrayList<>();
    private final List<Envelope<Event>> changes = new ArrayList<>();
    private final BufferedPublisher publisher = new BufferedPublisher();
    private final int originalVersion;
    private final Projection projection;

    public static ProcessManager start(UUID processId, Class<?> processType) {
        ProcessManager process = new ProcessManager(processId, processType, Collections.emptyList());
        // TODO: add Envelope.setContext() calls for causation ID, probably in ProcessManagers.handle()
        process.changes.add(Envelope.newMessage(new ProcessStarted(processId, processType)));
        return process;
    }

    public static ProcessManager load(List<Envelope<Event>> history) {
        ProcessStarted init = (ProcessStarted) history.get(0).payload;
        ProcessManager process = new ProcessManager(init.processId, init.processType, history);
        process.history.forEach(process.projection::apply);
        return process;
    }

    private ProcessManager(UUID processId, Class<?> processType, List<Envelope<Event>> history) {
        this.processId = processId;
        this.processType = processType;
        this.history.addAll(history);
        this.originalVersion = history.size();
        this.projection = newInstance(processType, publisher);
    }

    private static Projection newInstance(Class<?> processType, Publisher publisher) {
        try {
            return (Projection) processType.getConstructor(Publisher.class).newInstance(publisher);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate " + processType, e);
        }
    }

    public void handle(Envelope<Event> event) {
        Envelope.setContext(event); // TODO: should this be higher in the stack, in ProcessManagers.handle()?
        try {
            publisher.startRecording();
            changes.add(event);
            projection.apply(event);
        } finally {
            Envelope.resetContext();
        }
    }

    public void publishNewMessagesTo(MessageGateway target) {
        publisher.publishedMessages.forEach(target::send);
    }

    public int getOriginalVersion() {
        return originalVersion;
    }

    public void subscribe(UUID topic) {
        changes.add(Envelope.newMessage(new ProcessSubscribedToTopic(processId, topic)));
    }

    public void unsubscribe(UUID topic) {
        changes.add(Envelope.newMessage(new ProcessUnsubscribedFromTopic(processId, topic)));
    }

    public List<Envelope<Event>> getUncommittedChanges() {
        return Collections.unmodifiableList(changes);
    }

    public void markChangesAsCommitted() {
        // TODO: append changes to history and update originalVersion, so that this instance can be cached and reused?
        changes.clear();
    }

    private class BufferedPublisher implements Publisher {

        public List<Envelope<Message>> publishedMessages;

        public void startRecording() {
            publishedMessages = new ArrayList<>();
        }

        @Override
        public void publish(Message message) {
            if (publishedMessages != null) {
                publishedMessages.add(Envelope.newMessage(message).withCorrelationId(processId));
            }
        }
    }
}
