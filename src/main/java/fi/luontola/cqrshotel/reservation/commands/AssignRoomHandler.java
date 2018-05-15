// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.reservation.Reservation;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import fi.luontola.cqrshotel.room.queries.RoomsView;

public class AssignRoomHandler implements Handler<AssignRoom, Commit> {

    private final ReservationRepo repo;
    private final RoomsView roomsView;

    public AssignRoomHandler(ReservationRepo repo, RoomsView roomsView) {
        this.repo = repo;
        this.roomsView = roomsView;
    }

    @Override
    public Commit handle(AssignRoom command) {
        RoomDto room = roomsView.getById(command.roomId);
        Reservation reservation = repo.getById(command.reservationId);
        int originalVersion = reservation.getVersion();
        reservation.assignRoom(room.roomId, room.roomNumber);
        return repo.save(reservation, originalVersion);
    }
}
