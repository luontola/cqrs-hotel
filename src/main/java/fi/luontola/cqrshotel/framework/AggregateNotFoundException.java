// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.UUID;

public class AggregateNotFoundException extends RuntimeException {

    public AggregateNotFoundException(UUID aggrigateId) {
        super(String.valueOf(aggrigateId));
    }
}
