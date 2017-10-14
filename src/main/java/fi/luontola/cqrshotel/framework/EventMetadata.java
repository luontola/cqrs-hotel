// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class EventMetadata extends Struct {

    public UUID messageId;
    public UUID correlationId;
    public UUID causationId;
    public String type;
    public Integer version;
}
