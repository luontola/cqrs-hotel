// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.room.Room;
import fi.luontola.cqrshotel.room.RoomRepo;

public class CreateRoomHandler implements Handler<CreateRoom, Void> {

    private final RoomRepo repo;

    public CreateRoomHandler(RoomRepo repo) {
        this.repo = repo;
    }

    @Override
    public Void handle(CreateRoom command) {
        Room room = repo.create(command.roomId);
        int originalVersion = room.getVersion();
        room.createRoom(command.number);
        repo.save(room, originalVersion);
        return null;
    }
}
