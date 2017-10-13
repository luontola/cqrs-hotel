// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Projection;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatusPage {

    public EventStoreStatus eventStore;
    public Map<String, ProjectionStatus> projections;

    public static StatusPage build(EventStore eventStore, List<Projection> projections) {
        StatusPage status = new StatusPage();
        status.projections = projections.stream()
                .collect(Collectors.toMap(
                        ProjectionStatus::getName,
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
            EventStoreStatus status = new EventStoreStatus();
            status.position = eventStore.getCurrentPosition();
            return status;
        }
    }

    public static class ProjectionStatus {
        public Long position;

        private static ProjectionStatus build(Projection projection) {
            ProjectionStatus status = new ProjectionStatus();
            status.position = projection.getPosition();
            return status;
        }

        private static String getName(Projection projection) {
            return projection.getClass().getSimpleName();
        }
    }
}
