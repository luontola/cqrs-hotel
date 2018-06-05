// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class GetAvailabilityByTimeRangeHandler implements Handler<GetAvailabilityByTimeRange, RoomAvailabilityDto[]> {

    private final RoomAvailabilityView view;

    public GetAvailabilityByTimeRangeHandler(RoomAvailabilityView view) {
        this.view = view;
    }

    @Override
    public RoomAvailabilityDto[] handle(GetAvailabilityByTimeRange query) {
        return view.getAvailabilityForAllRooms(query.start, query.end).toArray(new RoomAvailabilityDto[0]);
    }
}
