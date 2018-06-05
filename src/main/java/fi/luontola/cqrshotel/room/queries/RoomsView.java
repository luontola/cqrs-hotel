// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.room.events.RoomCreated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoomsView extends AnnotatedProjection {

    private final ConcurrentMap<UUID, RoomDto> roomsById = new ConcurrentHashMap<>();

    @EventListener
    public void apply(RoomCreated event) {
        RoomDto room = new RoomDto();
        room.roomId = event.roomId;
        room.roomNumber = event.roomNumber;
        roomsById.put(room.roomId, room);
    }

    // queries

    public RoomDto getById(UUID roomId) {
        RoomDto room = roomsById.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("room not found: " + roomId);
        }
        return room;
    }

    public List<RoomDto> findAll() {
        return new ArrayList<>(roomsById.values());
    }
}
