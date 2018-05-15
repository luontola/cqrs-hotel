// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.room.NoRoomsAvailableException;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityIntervalDto;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;

import java.time.Instant;
import java.util.List;

public class OccupyAnyAvailableRoomHandler implements Handler<OccupyAnyAvailableRoom, Object> {

    private final RoomAvailabilityView roomAvailabilityView;
    private final Publisher publisher;

    public OccupyAnyAvailableRoomHandler(RoomAvailabilityView roomAvailabilityView, Publisher publisher) {
        this.roomAvailabilityView = roomAvailabilityView;
        this.publisher = publisher;
    }

    @Override
    public Commit handle(OccupyAnyAvailableRoom command) {
        RoomAvailabilityDto room = getAnyAvailableRoom(command.start, command.end);
        publisher.publish(new OccupyRoom(room.roomId, command.start, command.end, command.occupant));
        return null;
    }

    private RoomAvailabilityDto getAnyAvailableRoom(Instant start, Instant end) {
        for (RoomAvailabilityDto room : roomAvailabilityView.getAvailabilityForAllRooms(start, end)) {
            if (isFullyAvailable(room.availability)) {
                return room;
            }
        }
        throw new NoRoomsAvailableException();
    }

    private static boolean isFullyAvailable(List<RoomAvailabilityIntervalDto> intervals) {
        return intervals.size() == 1 && !intervals.get(0).occupied;
    }
}
