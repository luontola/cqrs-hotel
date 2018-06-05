// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.util;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UUIDs {

    private static final TimeBasedGenerator uuid1 = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    private static final Map<Class<?>, Field[]> uuidFieldsByType = new HashMap<>();

    public static UUID newUUID() {
        return uuid1.generate();
    }

    public static List<UUID> extractUUIDs(Object object) {
        Field[] fields = getUuidFields(object.getClass());
        return getUuidFieldValues(fields, object);
    }

    private static Field[] getUuidFields(Class<?> type) {
        return uuidFieldsByType.computeIfAbsent(type,
                t -> Arrays.stream(t.getFields())
                        .filter(field -> field.getType() == UUID.class
                                && !Modifier.isStatic(field.getModifiers()))
                        .toArray(Field[]::new));
    }

    private static List<UUID> getUuidFieldValues(Field[] fields, Object object) {
        try {
            List<UUID> values = new ArrayList<>(fields.length);
            for (Field field : fields) {
                UUID value = (UUID) field.get(object);
                if (value != null) {
                    values.add(value);
                }
            }
            return values;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
