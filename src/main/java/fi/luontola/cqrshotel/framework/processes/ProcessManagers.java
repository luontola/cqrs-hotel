// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.MessageGateway;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.framework.UUIDs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProcessManagers {

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
