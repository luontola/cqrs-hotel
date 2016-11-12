// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.commands;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class CreateRoom extends Struct implements Command {

    public final UUID roomId;
    public final String number;

    public CreateRoom(UUID roomId, String number) {
        this.roomId = roomId;
        this.number = number;
    }
}
