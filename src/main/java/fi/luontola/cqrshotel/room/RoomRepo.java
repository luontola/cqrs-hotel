// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Repository;

public class RoomRepo extends Repository<Room> {

    public RoomRepo(EventStore eventStore) {
        super(eventStore);
    }
}
