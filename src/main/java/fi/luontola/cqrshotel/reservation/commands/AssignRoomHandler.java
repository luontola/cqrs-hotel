// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.room.queries.GetRoomById;
import fi.luontola.cqrshotel.room.queries.RoomDto;

public class AssignRoomHandler implements Handler<AssignRoom, Commit> {

    private final ReservationRepo repo;
    private final Handler<GetRoomById, RoomDto> getRoomById;

    public AssignRoomHandler(ReservationRepo repo, Handler<GetRoomById, RoomDto> getRoomById) {
        this.repo = repo;
        this.getRoomById = getRoomById;
    }

    @Override
    public Commit handle(AssignRoom command) {
        var room = getRoomById.handle(new GetRoomById(command.roomId));
        var reservation = repo.getById(command.reservationId);
        var originalVersion = reservation.getVersion();
        reservation.assignRoom(room.roomId, room.roomNumber);
        return repo.save(reservation, originalVersion);
    }
}
