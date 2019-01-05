// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.BufferedPublisher;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoom;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoomHandler;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import fi.luontola.cqrshotel.room.queries.GetAvailabilityByTimeRangeHandler;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class OccupyAnyAvailableRoomTest {

    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();
    private static final Instant t1 = Instant.ofEpochSecond(1);
    private static final Instant t2 = Instant.ofEpochSecond(2);
    private static final Instant t3 = Instant.ofEpochSecond(3);
    private static final Instant t4 = Instant.ofEpochSecond(4);
    private static final UUID occupant = UUID.randomUUID();

    private final RoomAvailabilityView roomAvailabilityView = new RoomAvailabilityView();
    private final BufferedPublisher publisher = new BufferedPublisher();

    private final OccupyAnyAvailableRoomHandler commandHandler =
            new OccupyAnyAvailableRoomHandler(publisher, new GetAvailabilityByTimeRangeHandler(roomAvailabilityView));

    @Test
    public void occupies_an_available_room() {
        given(new RoomCreated(roomId, "101"));

        when(new OccupyAnyAvailableRoom(t1, t2, occupant));

        then(new OccupyRoom(roomId, t1, t2, occupant));
    }

    @Test
    public void occupies_only_one_room() {
        given(new RoomCreated(roomId, "101"),
                new RoomCreated(roomId2, "102"));

        when(new OccupyAnyAvailableRoom(t1, t2, occupant));

        thenPublishedMessages(hasSize(1));
    }

    @Test
    public void fails_if_rooms_have_no_availability() {
        given(new RoomCreated(roomId, "101"),
                new RoomOccupied(roomId, t1, t2, occupant));

        assertThrows(NoRoomsAvailableException.class, () -> {
            when(new OccupyAnyAvailableRoom(t1, t2, occupant));
        });
    }

    @Test
    public void fails_if_rooms_have_availability_only_for_part_of_the_requested_interval() {
        given(new RoomCreated(roomId, "101"),
                new RoomOccupied(roomId, t2, t3, occupant));

        assertThrows(NoRoomsAvailableException.class, () -> {
            when(new OccupyAnyAvailableRoom(t1, t4, occupant));
        });
    }


    // helpers

    private void given(Event... events) {
        for (var event : events) {
            roomAvailabilityView.apply(Envelope.newMessage(event));
        }
    }

    private void when(Command command) {
        commandHandler.handle((OccupyAnyAvailableRoom) command);
    }

    private void then(Message... expected) {
        assertThat(publisher.publishedMessages, is(Arrays.asList(expected)));
    }

    private void thenPublishedMessages(Matcher<Collection<? extends Message>> matcher) {
        assertThat(publisher.publishedMessages, matcher);
    }
}
