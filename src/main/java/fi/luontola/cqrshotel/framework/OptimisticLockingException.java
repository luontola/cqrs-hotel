// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException() {
    }

    public OptimisticLockingException(String message) {
        super(message);
    }
}
