// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.room.commands.ReserveRoom;
import fi.luontola.cqrshotel.room.commands.ReserveRoomHandler;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomReserved;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(FastTests.class)
public class ReserveRoomTest extends AggregateRootTester {

    {
        commandHandler = new ReserveRoomHandler(new RoomRepo(eventStore));
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void empty_room_can_be_reserved() {
        given(new RoomCreated(id, "123"));

        when(new ReserveRoom(id));

        then(new RoomReserved(id));
    }

    @Test
    public void room_cannot_be_reserved_twice() {
        given(new RoomCreated(id, "123"),
                new RoomReserved(id));

        thrown.expect(RoomAlreadyReservedException.class);
        when(new ReserveRoom(id));
    }
}
