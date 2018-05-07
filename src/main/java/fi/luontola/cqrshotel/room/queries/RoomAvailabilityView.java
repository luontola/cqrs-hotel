// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoomAvailabilityView extends Projection {

    private final Map<UUID, RoomAvailabilityDto> roomsById = new HashMap<>();

    public RoomAvailabilityView(EventStore eventStore) {
        super(eventStore);
    }

    public List<RoomAvailabilityDto> getAvailabilityForAllRooms(Instant start, Instant end) {
        return roomsById.values().stream()
                .map(src -> copyForResponse(src, start, end))
                .collect(Collectors.toList());
    }

    private static RoomAvailabilityDto copyForResponse(RoomAvailabilityDto src, Instant start, Instant end) {
        RoomAvailabilityDto response = new RoomAvailabilityDto();
        response.roomId = src.roomId;
        response.roomNumber = src.roomNumber;
        response.availability = availabilityForResponse(src, start, end);
        return response;
    }

    private static LinkedList<RoomAvailabilityIntervalDto> availabilityForResponse(RoomAvailabilityDto src, Instant queryStart, Instant queryEnd) {
        LinkedList<RoomAvailabilityIntervalDto> availability = src.availability.stream()
                // TODO: avoid linear search; use an O(log n) instead of O(n) algorithm
                .filter(interval -> interval.overlapsWith(queryStart, queryEnd))
                .collect(Collectors.toCollection(LinkedList::new));
        fillUnoccupiedIntervals(availability, queryStart, queryEnd);
        return availability;
    }

    private static void fillUnoccupiedIntervals(LinkedList<RoomAvailabilityIntervalDto> availability, Instant queryStart, Instant queryEnd) {
        Instant previousEnd = queryStart;
        for (ListIterator<RoomAvailabilityIntervalDto> iterator = availability.listIterator(); iterator.hasNext(); ) {
            RoomAvailabilityIntervalDto next = iterator.next();
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

    @EventListener
    public void apply(RoomCreated event) {
        RoomAvailabilityDto room = new RoomAvailabilityDto();
        room.roomId = event.roomId;
        room.roomNumber = event.roomNumber;
        room.availability = new LinkedList<>();
        roomsById.put(event.roomId, room);
    }

    @EventListener
    public void apply(RoomOccupied event) {
        RoomAvailabilityDto room = roomsById.get(event.roomId);
        // TODO: insert more efficiently; use an O(log n) instead of O(n * log n) algorithm
        room.availability.add(new RoomAvailabilityIntervalDto(event.start, event.end, true));
        room.availability.sort(Comparator.comparing(o -> o.start));
    }
}
