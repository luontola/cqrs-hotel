// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class QueryRouterTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void delegates_querys_to_the_handlers_by_query_type() {
        DummyQuery1Handler handler1 = new DummyQuery1Handler();
        DummyQuery2Handler handler2 = new DummyQuery2Handler();
        QueryRouter<Query> composite = new QueryRouter<>();
        composite.register(DummyQuery1.class, handler1);
        composite.register(DummyQuery2.class, handler2);
        DummyQuery1 query1 = new DummyQuery1();
        DummyQuery2 query2 = new DummyQuery2();

        Object result1 = composite.query(query1);
        Object result2 = composite.query(query2);

        assertThat("handler1.received", handler1.received, is(query1));
        assertThat("result1", result1, is("one"));
        assertThat("result2", result2, is("two"));
        assertThat("handler2.received", handler2.received, is(query2));
    }

    @Test
    public void cannot_register_two_handlers_for_the_same_query() {
        DummyQuery1Handler handler1 = new DummyQuery1Handler();
        DummyQuery1Handler handler2 = new DummyQuery1Handler();
        QueryRouter<Query> composite = new QueryRouter<>();

        composite.register(DummyQuery1.class, handler1);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("already registered");
        composite.register(DummyQuery1.class, handler2);
    }

    @Test
    public void fails_for_querys_not_registered() {
        QueryRouter<Query> composite = new QueryRouter<>();

        DummyQuery1 query = new DummyQuery1();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no handler");
        composite.query(query);
    }

    private static class DummyQuery1 implements Query {
    }

    private static class DummyQuery2 implements Query {
    }

    private static class DummyQuery1Handler implements Queries<DummyQuery1, String> {
        DummyQuery1 received;

        @Override
        public String query(DummyQuery1 query) {
            this.received = query;
            return "one";
        }
    }

    private static class DummyQuery2Handler implements Queries<DummyQuery2, String> {
        DummyQuery2 received;

        @Override
        public String query(DummyQuery2 query) {
            this.received = query;
            return "two";
        }
    }
}