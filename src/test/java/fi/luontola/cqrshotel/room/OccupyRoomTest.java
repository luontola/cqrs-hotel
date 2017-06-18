// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.commands.OccupyRoomHandler;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(FastTests.class)
public class OccupyRoomTest extends AggregateRootTester {

    {
        commandHandler = new OccupyRoomHandler(new RoomRepo(eventStore));
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void empty_room_can_be_occupied() {
        given(new RoomCreated(id, "123"));

        when(new OccupyRoom(id));

        then(new RoomOccupied(id));
    }

    // TODO: occupy a time range, not the whole room
    @Test
    public void occupied_room_cannot_be_occupied() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id));

        thrown.expect(RoomAlreadyOccupiedException.class);
        when(new OccupyRoom(id));
    }
}
