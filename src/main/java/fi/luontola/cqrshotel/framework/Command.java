// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

/**
 * Commands are requests to do a business-level operation.
 * Commands MAY be rejected if they fail business rule validation.
 * Commands MAY result in the creation of new {@link Event events} (zero to many).
 */
public interface Command extends Message {
}
