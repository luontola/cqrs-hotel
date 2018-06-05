// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.Repository;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;

public class ReservationRepo extends Repository<Reservation> {

    public ReservationRepo(EventStore eventStore) {
        super(eventStore);
    }
}
