// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.framework.eventstore.OptimisticLockingException;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.commands.CreateRoomHandler;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class CreateRoomTest extends AggregateRootTester {

    {
        commandHandler = new CreateRoomHandler(new RoomRepo(eventStore));
    }

    @Test
    public void creates_the_room() {
        when(new CreateRoom(id, "123"));

        then(new RoomCreated(id, "123"));
    }

    @Test
    public void cannot_create_the_room_twice() {
        given(new RoomCreated(id, "123"));

        assertThrows(OptimisticLockingException.class, () -> {
            when(new CreateRoom(id, "123"));
        });
    }
}
