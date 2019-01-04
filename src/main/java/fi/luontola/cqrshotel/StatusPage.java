// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.eventstore.EventStore;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatusPage {

    public EventStoreStatus eventStore;
    public Map<String, ProjectionStatus> projections;

    public static StatusPage build(EventStore eventStore, List<Core.ProjectionConfig<?>> projections) {
        var status = new StatusPage();
        status.projections = projections.stream()
                .collect(Collectors.toMap(
                        p -> p.projection.getProjectionName(),
                        ProjectionStatus::build,
                        (a, b) -> null,
                        TreeMap::new));
        // event store status needs to be calculated after projection status, or else
        // a projection's position could be higher than the event store's position
        status.eventStore = EventStoreStatus.build(eventStore);
        return status;
    }

    public static class EventStoreStatus {
        public Long position;

        public static EventStoreStatus build(EventStore eventStore) {
            var status = new EventStoreStatus();
            status.position = eventStore.getCurrentPosition();
            return status;
        }
    }

    public static class ProjectionStatus {
        public Long position;

        private static ProjectionStatus build(Core.ProjectionConfig<?> projection) {
            var status = new ProjectionStatus();
            status.position = projection.updater.getPosition();
            return status;
        }
    }
}
