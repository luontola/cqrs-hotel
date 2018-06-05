// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.MessageGateway;
import fi.luontola.cqrshotel.framework.projections.Projection;
import fi.luontola.cqrshotel.framework.util.UUIDs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ProcessManagers {

    private final ProcessRepo repo;
    private final MessageGateway gateway;
    private final List<EntryPoint> entryPoints = new ArrayList<>();

    public ProcessManagers(ProcessRepo repo, MessageGateway gateway) {
        this.repo = repo;
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
        for (UUID processId : findSubscribedProcesses(event)) {
            delegateToProcess(processId, event);
        }
    }

    private void delegateToProcess(UUID processId, Envelope<Event> event) {
        ProcessManager process = repo.getById(processId);
        process.handle(event);
        repo.save(process);
        process.publishNewMessagesTo(gateway);
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
        // FIXME: if there is a crash and the initial event is processed again, then this may start duplicate processes
        // Possible solutions:
        // - do not save the process yet here, but only after handing the initial event (would avoid subscription to the first message's ID)
        // - use the initial event's message ID (or a deterministically derived ID) as the process ID
        UUID processId = UUIDs.newUUID();
        ProcessManager process = repo.create(processId, processType);
        process.subscribe(processId); // subscribe to itself as correlationId to receive responses to own commands
        process.subscribe(initialEvent.messageId); // always handle the first message (it won't have processId as correlationId)
        repo.save(process);
    }

    // lookup

    private Set<UUID> findSubscribedProcesses(Envelope<Event> event) {
        List<UUID> topics = getTopics(event);
        return repo.findSubscribersToAnyOf(topics);
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
