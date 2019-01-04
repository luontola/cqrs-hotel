// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Publisher;
import fi.luontola.cqrshotel.room.NoRoomsAvailableException;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByTimeRange;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityDto;

import java.time.Instant;
import java.util.stream.Stream;

public class OccupyAnyAvailableRoomHandler implements Handler<OccupyAnyAvailableRoom, Commit> {

    private final Publisher publisher;
    private final Handler<GetAvailabilityByTimeRange, RoomAvailabilityDto[]> getAvailabilityByTimeRange;

    public OccupyAnyAvailableRoomHandler(Publisher publisher, Handler<GetAvailabilityByTimeRange, RoomAvailabilityDto[]> getAvailabilityByTimeRange) {
        this.publisher = publisher;
        this.getAvailabilityByTimeRange = getAvailabilityByTimeRange;
    }

    @Override
    public Commit handle(OccupyAnyAvailableRoom command) {
        var room = getAnyAvailableRoom(command.start, command.end);
        publisher.publish(new OccupyRoom(room.roomId, command.start, command.end, command.occupant));
        return null;
    }

    private RoomAvailabilityDto getAnyAvailableRoom(Instant start, Instant end) {
        return Stream.of(getAvailabilityByTimeRange.handle(new GetAvailabilityByTimeRange(start, end)))
                .filter(room -> room.available)
                .findAny()
                .orElseThrow(NoRoomsAvailableException::new);
    }
}
