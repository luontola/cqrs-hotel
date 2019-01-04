// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoomAvailabilityView extends AnnotatedProjection {

    private final Map<UUID, RoomAvailabilityDto> roomsById = new HashMap<>();

    @EventListener
    public void apply(RoomCreated event) {
        var room = new RoomAvailabilityDto();
        room.roomId = event.roomId;
        room.roomNumber = event.roomNumber;
        room.details = new LinkedList<>();
        roomsById.put(event.roomId, room);
    }

    @EventListener
    public void apply(RoomOccupied event) {
        var room = roomsById.get(event.roomId);
        // TODO: insert more efficiently; use an O(log n) instead of O(n * log n) algorithm
        room.details.add(new RoomAvailabilityIntervalDto(event.start, event.end, true));
        room.details.sort(Comparator.comparing(o -> o.start));
    }

    // queries

    public List<RoomAvailabilityDto> getAvailabilityForAllRooms(Instant start, Instant end) {
        return roomsById.values().stream()
                .map(src -> copyForResponse(src, start, end))
                .collect(Collectors.toList());
    }

    // helpers

    private static RoomAvailabilityDto copyForResponse(RoomAvailabilityDto src, Instant start, Instant end) {
        var response = new RoomAvailabilityDto();
        response.roomId = src.roomId;
        response.roomNumber = src.roomNumber;
        response.details = availabilityForResponse(src, start, end);
        response.available = isFullyAvailable(response.details);
        return response;
    }

    private static LinkedList<RoomAvailabilityIntervalDto> availabilityForResponse(RoomAvailabilityDto src, Instant queryStart, Instant queryEnd) {
        var availability = src.details.stream()
                // TODO: avoid linear search; use an O(log n) instead of O(n) algorithm
                .filter(interval -> interval.overlapsWith(queryStart, queryEnd))
                .collect(Collectors.toCollection(LinkedList::new));
        fillUnoccupiedIntervals(availability, queryStart, queryEnd);
        return availability;
    }

    private static void fillUnoccupiedIntervals(LinkedList<RoomAvailabilityIntervalDto> availability, Instant queryStart, Instant queryEnd) {
        var previousEnd = queryStart;
        for (var iterator = availability.listIterator(); iterator.hasNext(); ) {
            var next = iterator.next();
            if (previousEnd.isBefore(next.start)) {
                // there is a gap before the next interval
                iterator.previous();
                iterator.add(new RoomAvailabilityIntervalDto(previousEnd, next.start, false));
            }
            previousEnd = next.end;
        }
        if (previousEnd.isBefore(queryEnd)) {
            // there is a gap after the last interval
            availability.addLast(new RoomAvailabilityIntervalDto(previousEnd, queryEnd, false));
        }
    }

    private static boolean isFullyAvailable(List<RoomAvailabilityIntervalDto> intervals) {
        return intervals.size() == 1 && !intervals.get(0).occupied;
    }
}
