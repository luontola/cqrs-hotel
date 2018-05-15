// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room;

import fi.luontola.cqrshotel.framework.AggregateRoot;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.events.RoomOccupied;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room extends AggregateRoot {

    private final List<Range> occupiedRanges = new ArrayList<>();

    @EventListener
    private void apply(RoomOccupied event) {
        occupiedRanges.add(new Range(event.start, event.end));
    }

    public void createRoom(String number) {
        publish(new RoomCreated(getId(), number));
    }

    public void occupy(Range range, UUID occupant) {
        if (isOccupiedAt(range)) {
            throw new RoomAlreadyOccupiedException();
        }
        publish(new RoomOccupied(getId(), range.start, range.end, occupant));
    }

    private boolean isOccupiedAt(Range range) {
        return occupiedRanges.stream().anyMatch(range::overlaps);
    }
}
