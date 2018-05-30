// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Category(FastTests.class)
public class UUIDsTest {

    private static final UUID id1 = UUID.randomUUID();
    private static final UUID id2 = UUID.randomUUID();

    @Test
    public void generates_unique_time_based_UUIDs() {
        UUID uuid = UUIDs.newUUID();
        UUID anotherUuid = UUIDs.newUUID();
        assertThat(uuid, is(not(anotherUuid)));
        assertThat(uuid.version(), is(1));
    }

    @Test
    public void extracts_UUIDs_from_all_public_instance_fields() {
        @SuppressWarnings("unused")
        class GuineaPig {
            public UUID foo = id1;
            public UUID bar = id2;
        }
        GuineaPig obj = new GuineaPig();
        assertThat(UUIDs.extractUUIDs(obj), containsInAnyOrder(id1, id2));
    }

    @Test
    public void extracting_UUIDs_ignores_other_fields() {
        IgnoredFieldsGuineaPig obj = new IgnoredFieldsGuineaPig();
        assertThat(UUIDs.extractUUIDs(obj), is(empty()));
    }

    @SuppressWarnings("unused")
    static class IgnoredFieldsGuineaPig {
        public UUID nullValue = null;
        public Object notDeclaredUuid = id1;
        public String notUuid = "bar";
        public static UUID notInstanceField = id1;
        UUID notPublic1 = id1;
        private UUID notPublic2 = id1;
        protected UUID notPublic3 = id1;
    }
}
