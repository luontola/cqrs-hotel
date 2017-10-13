// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

/**
 * Events are business-level things that have happened in past.
 * Events MUST NOT be changed after they have been published.
 * Events are persisted and are always read in the same order that the events happened.
 */
public interface Event extends Message {
}
