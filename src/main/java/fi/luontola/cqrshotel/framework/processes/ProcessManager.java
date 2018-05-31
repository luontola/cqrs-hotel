// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.BufferedPublisher;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.MessageGateway;
import fi.luontola.cqrshotel.framework.Projection;

import java.util.UUID;

public class ProcessManager { // TODO: merge ProcessManager and PersistedProcess?

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
        BufferedPublisher publisher = new BufferedPublisher();
        PersistedProcess state = processRepo.getById(processId);
        Projection process = state.newInstance(publisher);
        state.history.forEach(oldEvent -> {
            process.apply(oldEvent);
            publisher.publishedMessages.clear(); // avoid republishing historical messages
        });

        process.apply(event);

        processRepo.save(processId, event);
        publisher.publishedMessages.stream()
                .map(m -> Envelope.newMessage(m, event).withCorrelationId(processId))
                .forEach(gateway::send);
    }
}
