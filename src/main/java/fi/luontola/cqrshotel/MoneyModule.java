// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.javamoney.moneta.Money;

import java.io.IOException;

public class MoneyModule extends SimpleModule {

    public MoneyModule() {
        addSerializer(Money.class, new MoneySerializer());
        addDeserializer(Money.class, new MoneyDeserializer());
    }

    private static class MoneySerializer extends StdScalarSerializer<Money> {

        public MoneySerializer() {
            super(Money.class);
        }

        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    private static class MoneyDeserializer extends FromStringDeserializer<Money> {

        public MoneyDeserializer() {
            super(Money.class);
        }

        @Override
        protected Money _deserialize(String value, DeserializationContext ctxt) throws IOException {
            return Money.parse(value);
        }
    }
}
