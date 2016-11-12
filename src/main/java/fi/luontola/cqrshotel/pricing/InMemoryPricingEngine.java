// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.pricing;

import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryPricingEngine implements PricingEngine {

    private final Map<LocalDate, Money> prices = new HashMap<>();

    @Override
    public Optional<Money> getAccommodationPrice(LocalDate date) {
        return Optional.ofNullable(prices.get(date));
    }

    public InMemoryPricingEngine setPrice(LocalDate date, Money price) {
        prices.put(date, price);
        return this;
    }
}
