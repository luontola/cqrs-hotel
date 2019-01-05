// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.luontola.cqrshotel.Application;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = NONE)
@ActiveProfiles("test")
@Tag("slow")
public class PsqlEventStoreTest extends EventStoreContract {

    @Autowired
    DataSource dataSource;

    @Autowired
    ObjectMapper objectMapper;

    public void init() {
        eventStore = new PsqlEventStore(dataSource, objectMapper);
    }
}