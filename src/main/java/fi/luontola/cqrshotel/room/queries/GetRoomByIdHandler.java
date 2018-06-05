// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class GetRoomByIdHandler implements Handler<GetRoomById, RoomDto> {

    private final RoomsView roomsView;

    public GetRoomByIdHandler(RoomsView roomsView) {
        this.roomsView = roomsView;
    }

    @Override
    public RoomDto handle(GetRoomById query) {
        return roomsView.getById(query.roomId);
    }
}
