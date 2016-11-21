// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.experimental.categories.Category;

@Category(FastTests.class)
public class InMemoryEventStoreTest extends EventStoreContract {

    @Override
    protected void init() {
        eventStore = new InMemoryEventStore();
    }
}