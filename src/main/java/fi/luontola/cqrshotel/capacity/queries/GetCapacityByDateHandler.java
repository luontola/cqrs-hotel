// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.capacity.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class GetCapacityByDateHandler implements Handler<GetCapacityByDate, CapacityDto> {

    private final CapacityView capacityView;

    public GetCapacityByDateHandler(CapacityView capacityView) {
        this.capacityView = capacityView;
    }

    @Override
    public CapacityDto handle(GetCapacityByDate query) {
        return capacityView.getCapacityByDate(query.date);
    }
}
