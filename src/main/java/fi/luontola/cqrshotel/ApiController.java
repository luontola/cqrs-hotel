// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class ApiController {

    @RequestMapping("/api")
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping("/api/dummy")
    public List<String> dummy() {
        return Arrays.asList("foo", "bar", "gazonk");
    }
}
