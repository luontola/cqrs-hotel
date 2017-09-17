// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.util.Struct;

import java.time.LocalDate;
import java.util.UUID;

public class ReservationDto extends Struct {

    public UUID reservationId;
    public LocalDate arrival;
    public LocalDate departure;
    public String checkInTime;
    public String checkOutTime;
    public String name;
    public String email;
    public String status;
}
