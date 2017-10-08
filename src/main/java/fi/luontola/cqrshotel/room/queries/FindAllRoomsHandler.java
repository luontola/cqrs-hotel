// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class FindAllRoomsHandler implements Handler<FindAllRooms, RoomDto[]> {

    private final RoomsView roomsView;

    public FindAllRoomsHandler(RoomsView roomsView) {
        this.roomsView = roomsView;
    }

    @Override
    public RoomDto[] handle(FindAllRooms query) {
        return roomsView.findAll().toArray(new RoomDto[0]);
    }
}
