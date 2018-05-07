// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class GetAvailabilityByDateRangeHandlerTest {

    private final RoomAvailabilityView view = new RoomAvailabilityView(new InMemoryEventStore());
    private final GetAvailabilityByDateRangeHandler handler = new GetAvailabilityByDateRangeHandler(view);

    @Test
    public void queries_from_start_of_day_to_end_of_day_in_hotel_timezone() {
        view.apply(new RoomCreated(UUID.randomUUID(), "101"));

        RoomAvailabilityDto[] result = handler.handle(new GetAvailabilityByDateRange(LocalDate.parse("2000-01-01"), LocalDate.parse("2000-01-05")));

        RoomAvailabilityIntervalDto interval = result[0].availability.get(0);
        assertThat("start", interval.start, is(Instant.parse("1999-12-31T22:00:00Z")));
        assertThat("end", interval.end, is(Instant.parse("2000-01-05T22:00:00Z")));
    }
}
