// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.commands.OccupyRoomHandler;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class OccupyRoomTest extends AggregateRootTester {

    private static final UUID occupant = UUID.randomUUID();
    private static final Instant t1 = Instant.ofEpochSecond(1);
    private static final Instant t2 = Instant.ofEpochSecond(2);
    private static final Instant t3 = Instant.ofEpochSecond(3);
    private static final Instant t4 = Instant.ofEpochSecond(4);

    {
        commandHandler = new OccupyRoomHandler(new RoomRepo(eventStore));
    }

    @Test
    public void empty_room_can_be_occupied() {
        given(new RoomCreated(id, "123"));

        when(new OccupyRoom(id, t1, t2, occupant));

        then(new RoomOccupied(id, t1, t2, occupant));
    }

    @Test
    public void can_occupy_room_at_disjoined_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t1, t2, occupant));

        when(new OccupyRoom(id, t3, t4, occupant));

        then(new RoomOccupied(id, t3, t4, occupant));
    }

    @Test
    public void can_occupy_room_at_adjacent_earlier_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t2, t3, occupant));

        when(new OccupyRoom(id, t1, t2, occupant));

        then(new RoomOccupied(id, t1, t2, occupant));
    }

    @Test
    public void can_occupy_room_at_adjacent_later_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t2, t3, occupant));

        when(new OccupyRoom(id, t3, t4, occupant));

        then(new RoomOccupied(id, t3, t4, occupant));
    }

    @Test
    public void cannot_occupy_room_at_exactly_same_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t1, t2, occupant));

        assertThrows(RoomAlreadyOccupiedException.class, () -> {
            when(new OccupyRoom(id, t1, t2, occupant));
        });
    }

    @Test
    public void cannot_occupy_room_at_subset_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t1, t4, occupant));

        assertThrows(RoomAlreadyOccupiedException.class, () -> {
            when(new OccupyRoom(id, t2, t3, occupant));
        });
    }

    @Test
    public void cannot_occupy_room_at_overlapping_earlier_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t2, t4, occupant));

        assertThrows(RoomAlreadyOccupiedException.class, () -> {
            when(new OccupyRoom(id, t1, t3, occupant));
        });
    }

    @Test
    public void cannot_occupy_room_at_overlapping_later_time() {
        given(new RoomCreated(id, "123"),
                new RoomOccupied(id, t1, t3, occupant));

        assertThrows(RoomAlreadyOccupiedException.class, () -> {
            when(new OccupyRoom(id, t2, t4, occupant));
        });
    }

    @Test
    public void start_cannot_equal_end() {
        given(new RoomCreated(id, "123"));

        var e = assertThrows(IllegalArgumentException.class, () -> {
            when(new OccupyRoom(id, t1, t1, occupant));
        });
        assertThat(e.getMessage(), is("start must be before end, but was: start 1970-01-01T00:00:01Z, end 1970-01-01T00:00:01Z"));
    }

    @Test
    public void start_cannot_be_after_end() {
        given(new RoomCreated(id, "123"));

        var e = assertThrows(IllegalArgumentException.class, () -> {
            when(new OccupyRoom(id, t2, t1, occupant));
        });
        assertThat(e.getMessage(), is("start must be before end, but was: start 1970-01-01T00:00:02Z, end 1970-01-01T00:00:01Z"));
    }
}
