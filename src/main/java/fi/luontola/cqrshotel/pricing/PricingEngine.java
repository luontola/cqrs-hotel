// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.pricing;

import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.Optional;

public interface PricingEngine {

    Optional<Money> getAccommodationPrice(LocalDate date);
}
