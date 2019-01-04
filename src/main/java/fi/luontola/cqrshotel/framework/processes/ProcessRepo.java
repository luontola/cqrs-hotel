// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.eventstore.OptimisticLockingException;
import fi.luontola.cqrshotel.framework.processes.events.ProcessSubscribedToTopic;
import fi.luontola.cqrshotel.framework.processes.events.ProcessUnsubscribedFromTopic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProcessRepo {

    // TODO: create a psql version

    private final Map<UUID, List<Envelope<Event>>> processesById = new HashMap<>();
    private final SetMultimap<UUID, UUID> subscribedProcessesByTopic = HashMultimap.create();

    public ProcessManager create(UUID processId, Class<?> processType) {
        return ProcessManager.start(processId, processType);
    }

    public void save(ProcessManager process) {
        var processId = process.processId;
        var savedEvents = processesById.computeIfAbsent(processId, key -> new ArrayList<>());

        var expectedVersion = process.getOriginalVersion();
        var actualVersion = savedEvents.size();
        if (expectedVersion != actualVersion) {
            throw new OptimisticLockingException("expected version " + expectedVersion + " but was " + actualVersion + " for process " + processId);
        }

        var changes = process.getUncommittedChanges();
        for (var change : changes) {
            // TODO: use EventListeners?

            if (change.payload instanceof ProcessSubscribedToTopic) {
                var event = (ProcessSubscribedToTopic) change.payload;
                subscribe(event.processId, event.topic);

            } else if (change.payload instanceof ProcessUnsubscribedFromTopic) {
                var event = (ProcessUnsubscribedFromTopic) change.payload;
                unsubscribe(event.processId, event.topic);
            }
        }

        savedEvents.addAll(changes);
        process.markChangesAsCommitted();
    }

    public ProcessManager getById(UUID processId) {
        var events = processesById.computeIfAbsent(processId, key -> {
            throw new IllegalArgumentException("Process not found: " + key);
        });
        return ProcessManager.load(events);
    }

    private void subscribe(UUID processId, UUID topic) {
        subscribedProcessesByTopic.put(topic, processId);
    }

    private void unsubscribe(UUID processId, UUID topic) {
        subscribedProcessesByTopic.remove(topic, processId);
    }

    public Set<UUID> findSubscribersToAnyOf(UUID... topics) {
        return findSubscribersToAnyOf(Arrays.asList(topics));
    }

    public Set<UUID> findSubscribersToAnyOf(List<UUID> topics) {
        Set<UUID> processIds = new HashSet<>();
        for (var topic : topics) {
            processIds.addAll(subscribedProcessesByTopic.get(topic));
        }
        return processIds;
    }
}
