// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.hotel.Hotel;

import java.time.Instant;

public class GetAvailabilityByDateRangeHandler implements Handler<GetAvailabilityByDateRange, RoomAvailabilityDto[]> {

    private final RoomAvailabilityView view;

    public GetAvailabilityByDateRangeHandler(RoomAvailabilityView view) {
        this.view = view;
    }

    @Override
    public RoomAvailabilityDto[] handle(GetAvailabilityByDateRange query) {
        Instant start = query.start.atStartOfDay(Hotel.TIMEZONE).toInstant();
        Instant end = query.end.plusDays(1).atStartOfDay(Hotel.TIMEZONE).toInstant();
        return view.getAvailabilityForAllRooms(start, end).toArray(new RoomAvailabilityDto[0]);
    }
}
