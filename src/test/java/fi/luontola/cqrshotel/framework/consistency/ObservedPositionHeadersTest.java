// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ObservedPositionHeadersTest.GuineaPigApp.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Tag("slow")
public class ObservedPositionHeadersTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void reads_observed_position_from_request_headers() {
        var headers = new HttpHeaders();
        headers.add(ObservedPosition.HTTP_HEADER, "42");
        var response = restTemplate.exchange("/getObservedPosition", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getBody(), is("42"));
    }

    @Test
    public void writes_observed_position_to_response_headers() {
        var response = restTemplate.exchange("/setObservedPosition/2501", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getHeaders().getFirst(ObservedPosition.HTTP_HEADER), is("2501"));
    }

    @Test
    public void sends_error_503_Service_Unavailable_if_read_model_is_not_up_to_date() {
        var response = restTemplate.exchange("/readModelNotUpToDate", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.SERVICE_UNAVAILABLE));
    }


    @SpringBootApplication
    public static class GuineaPigApp {

        @Bean
        public ObservedPosition observedPosition() {
            return new ObservedPosition(Duration.ZERO);
        }

        @Bean
        public GuineaPigController guineaPigController() {
            return new GuineaPigController();
        }
    }

    @RestController
    public static class GuineaPigController {

        @Autowired
        ObservedPosition observedPosition;

        @GetMapping("/getObservedPosition")
        public String getObservedPosition() {
            return String.valueOf(observedPosition.get());
        }

        @GetMapping("/setObservedPosition/{position}")
        public String setObservedPosition(@PathVariable Long position) {
            observedPosition.observe(position);
            return "ok";
        }

        @GetMapping("/readModelNotUpToDate")
        public String readModelNotUpToDate() {
            throw new ReadModelNotUpToDateException();
        }
    }
}
