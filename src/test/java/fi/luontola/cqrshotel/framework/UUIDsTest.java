// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Category(FastTests.class)
public class UUIDsTest {

    @Test
    public void generates_unique_time_based_UUIDs() {
        UUID uuid = UUIDs.newUUID();
        UUID anotherUuid = UUIDs.newUUID();
        assertThat(uuid, is(not(anotherUuid)));
        assertThat(uuid.version(), is(1));
    }
}
