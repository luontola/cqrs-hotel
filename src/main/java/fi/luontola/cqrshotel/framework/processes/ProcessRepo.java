// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProcessRepo {

    // TODO: write contract tests
    // TODO: create a psql version

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
