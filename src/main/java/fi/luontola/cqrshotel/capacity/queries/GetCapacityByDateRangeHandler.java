// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.capacity.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class GetCapacityByDateRangeHandler implements Handler<GetCapacityByDateRange, CapacityDto[]> {

    private final CapacityView capacityView;

    public GetCapacityByDateRangeHandler(CapacityView capacityView) {
        this.capacityView = capacityView;
    }

    @Override
    public CapacityDto[] handle(GetCapacityByDateRange query) {
        return capacityView.getCapacityByDateRange(query.start, query.end).toArray(new CapacityDto[0]);
    }
}
