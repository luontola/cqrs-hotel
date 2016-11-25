// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.hotel;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.time.LocalTime;
import java.time.ZoneId;

public class Hotel {

    public static final CurrencyUnit CURRENCY = Monetary.getCurrency("EUR");
    public static final ZoneId TIMEZONE = ZoneId.systemDefault();
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final LocalTime CHECK_OUT_TIME = LocalTime.of(12, 0);
}
