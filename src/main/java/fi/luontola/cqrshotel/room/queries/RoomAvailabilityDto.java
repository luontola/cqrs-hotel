// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.room.queries;

import fi.luontola.cqrshotel.util.Struct;

import java.util.List;
import java.util.UUID;

public class RoomAvailabilityDto extends Struct {

    public UUID roomId;
    public String roomNumber;
    public Boolean available;
    public List<RoomAvailabilityIntervalDto> details;
}
