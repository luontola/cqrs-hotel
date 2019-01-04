// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.room.RoomRepo;

public class CreateRoomHandler implements Handler<CreateRoom, Commit> {

    private final RoomRepo repo;

    public CreateRoomHandler(RoomRepo repo) {
        this.repo = repo;
    }

    @Override
    public Commit handle(CreateRoom command) {
        var room = repo.create(command.roomId);
        var originalVersion = room.getVersion();
        room.createRoom(command.number);
        return repo.save(room, originalVersion);
    }
}
