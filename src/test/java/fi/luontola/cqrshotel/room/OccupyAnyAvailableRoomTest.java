// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.framework.SpyPublisher;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoom;
import fi.luontola.cqrshotel.room.commands.OccupyAnyAvailableRoomHandler;
import fi.luontola.cqrshotel.room.commands.OccupyRoom;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;
import fi.luontola.cqrshotel.room.queries.RoomAvailabilityView;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class OccupyAnyAvailableRoomTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();
    private static final Instant t1 = Instant.ofEpochSecond(1);
    private static final Instant t2 = Instant.ofEpochSecond(2);
    private static final Instant t3 = Instant.ofEpochSecond(3);
    private static final Instant t4 = Instant.ofEpochSecond(4);
    private static final UUID occupant = UUID.randomUUID();

    private final RoomAvailabilityView roomAvailabilityView = new RoomAvailabilityView();
    private final SpyPublisher publisher = new SpyPublisher();

    private final OccupyAnyAvailableRoomHandler commandHandler = new OccupyAnyAvailableRoomHandler(roomAvailabilityView, publisher);

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

        thrown.expect(NoRoomsAvailableException.class);
        when(new OccupyAnyAvailableRoom(t1, t2, occupant));
    }

    @Test
    public void fails_if_rooms_have_availability_only_for_part_of_the_requested_interval() {
        given(new RoomCreated(roomId, "101"),
                new RoomOccupied(roomId, t2, t3, occupant));

        thrown.expect(NoRoomsAvailableException.class);
        when(new OccupyAnyAvailableRoom(t1, t4, occupant));
    }


    // helpers

    private void given(Event... events) {
        for (Event event : events) {
            roomAvailabilityView.apply(event);
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
