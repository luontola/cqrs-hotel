// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.luontola.cqrshotel.Application;
import fi.luontola.cqrshotel.SlowTests;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = NONE)
@ActiveProfiles("test")
@Category(SlowTests.class)
public class PsqlEventStoreTest extends EventStoreContract {

    @Autowired
    DataSource dataSource;

    @Autowired
    ObjectMapper objectMapper;

    public void init() {
        eventStore = new PsqlEventStore(dataSource, objectMapper);
    }
}