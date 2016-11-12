// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.util.Struct;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.UUID;

public class ReservationOffer extends Struct {

    public UUID reservationId;
    public LocalDate startDate;
    public LocalDate endDate;
    public Money totalPrice;
}
