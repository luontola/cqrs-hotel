// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class RoomsViewTest {

    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();

    private final RoomsView view = new RoomsView(new InMemoryEventStore());

    @Test
    public void fills_in_all_fields() {
        view.apply(new RoomCreated(roomId, "123"));

        RoomDto expected = new RoomDto();
        expected.roomId = roomId;
        expected.roomNumber = "123";

        assertThat(view.findAll(), is(Collections.singletonList(expected)));
    }

    @Test
    public void lists_all_rooms() {
        view.apply(new RoomCreated(roomId, "101"));
        view.apply(new RoomCreated(roomId2, "102"));

        List<RoomDto> results = view.findAll();
        assertThat(results, hasSize(2));
    }
}
