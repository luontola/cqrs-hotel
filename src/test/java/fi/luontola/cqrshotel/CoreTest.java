// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.consistency.ReadModelNotUpToDateException;
import fi.luontola.cqrshotel.framework.eventstore.InMemoryEventStore;
import fi.luontola.cqrshotel.pricing.InMemoryPricingEngine;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.FindAllRooms;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class CoreTest {

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final InMemoryPricingEngine pricing = new InMemoryPricingEngine();
    private final Clock clock = Clock.systemDefaultZone();
    private final ObservedPosition observedPosition = new ObservedPosition(Duration.ofSeconds(5));
    private final Core core = new Core(eventStore, pricing, clock, observedPosition);

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
        var rooms = (RoomDto[]) core.handle(new FindAllRooms());

        assertThat(rooms, is(arrayWithSize(1)));
        assertThat(rooms[0].roomNumber, is("123"));
    }

    @Test
    public void throws_exception_if_projection_is_not_up_to_date_query_after_timeout() {
        var observedPosition = new ObservedPosition(Duration.ofSeconds(0));
        var core = new Core(eventStore, pricing, clock, observedPosition);
        core.handle(new CreateRoom(UUID.randomUUID(), "123"));

        observedPosition.observe(observedPosition.get() + 1); // wait for a future that will never arrive

        assertThrows(ReadModelNotUpToDateException.class, () -> {
            core.handle(new FindAllRooms());
        });
    }

    @Test
    public void status_page() {
        var status = core.getStatus();

        assertThat("eventStore.position", status.eventStore.position, is(notNullValue()));

        var projection = status.projections.get("RoomsView");
        assertThat("projection", projection, is(notNullValue()));
        assertThat("projection.position", projection.position, is(notNullValue()));
    }
}
