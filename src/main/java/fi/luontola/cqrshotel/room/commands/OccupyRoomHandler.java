// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.room.Room;
import fi.luontola.cqrshotel.room.RoomRepo;

public class OccupyRoomHandler implements Handler<OccupyRoom, Void> {

    private final RoomRepo repo;

    public OccupyRoomHandler(RoomRepo repo) {
        this.repo = repo;
    }

    @Override
    public Void handle(OccupyRoom command) {
        Room room = repo.getById(command.roomId);
        int originalVersion = room.getVersion();
        room.occupy();
        repo.save(room, originalVersion);
        return null;
    }
}
