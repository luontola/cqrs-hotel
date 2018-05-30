// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UUIDs {

    private static final TimeBasedGenerator uuid1 = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    private static final Map<Class<?>, Field[]> uuidFieldsByType = new HashMap<>();

    public static UUID newUUID() {
        return uuid1.generate();
    }
}
