// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.StatusPage.ProjectionStatus;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.consistency.ReadModelNotUpToDateException;
import fi.luontola.cqrshotel.pricing.InMemoryPricingEngine;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.FindAllRooms;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Category(FastTests.class)
public class CoreTest {

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final InMemoryPricingEngine pricing = new InMemoryPricingEngine();
    private final Clock clock = Clock.systemDefaultZone();
    private final ObservedPosition observedPosition = new ObservedPosition(Duration.ofSeconds(5));
    private final Core core = new Core(eventStore, pricing, clock, observedPosition);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void updates_observed_position_after_command() {
        assertThat(observedPosition.get(), is(0L));

        core.handle(new CreateRoom(UUID.randomUUID(), "123"));

        assertThat(observedPosition.get(), is(1L));
    }

    @Test
    public void updates_observed_position_after_query() {
        core.handle(new CreateRoom(UUID.randomUUID(), "123"));
        core.handle(new FindAllRooms()); // wait for projection to update
        observedPosition.reset();
        assertThat(observedPosition.get(), is(0L));

        core.handle(new FindAllRooms());

        assertThat(observedPosition.get(), is(1L));
    }

    @Test
    public void waits_for_projection_to_update_before_query() {
        core.handle(new CreateRoom(UUID.randomUUID(), "123"));

        // projections are update asynchronously, so this must block based on observed position
        RoomDto[] rooms = (RoomDto[]) core.handle(new FindAllRooms());

        assertThat(rooms, is(arrayWithSize(1)));
        assertThat(rooms[0].roomNumber, is("123"));
    }

    @Test
    public void throws_exception_if_projection_is_not_up_to_date_query_after_timeout() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ofSeconds(0));
        Core core = new Core(eventStore, pricing, clock, observedPosition);
        synchronized (core.roomsView) { // grab a lock on the projection to prevent it being updated asynchronously

            core.handle(new CreateRoom(UUID.randomUUID(), "123"));

            thrown.expect(ReadModelNotUpToDateException.class);
            core.handle(new FindAllRooms());
        }
    }

    @Test
    public void status_page() {
        StatusPage status = core.getStatus();

        assertThat("eventStore.position", status.eventStore.position, is(notNullValue()));

        ProjectionStatus projection = status.projections.get("RoomsView");
        assertThat("projection", projection, is(notNullValue()));
        assertThat("projection.position", projection.position, is(notNullValue()));
    }
}
