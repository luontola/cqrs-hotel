// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.Application;
import fi.luontola.cqrshotel.SlowTests;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.util.concurrent.ExecutionException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = NONE)
@Category(SlowTests.class)
public class PsqlEventStoreTest extends EventStoreContract {

    @Autowired
    DataSource dataSource;

    public void init() {
        eventStore = new PsqlEventStore(dataSource);
    }

    @Ignore // TODO: not implemented
    @Test
    @Override
    public void concurrent_writers_to_same_stream() throws ExecutionException, InterruptedException {
        super.concurrent_writers_to_same_stream();
    }
}