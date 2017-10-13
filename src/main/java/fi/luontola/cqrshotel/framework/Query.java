// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

/**
 * Queries are requests to read data from the system.
 * Queries MUST NOT modify state.
 */
public interface Query extends Message {
}
