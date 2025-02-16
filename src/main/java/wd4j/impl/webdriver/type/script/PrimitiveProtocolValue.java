package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;

@JsonAdapter(PrimitiveProtocolValue.PrimitiveProtocolValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface PrimitiveProtocolValue<T> {
    Type getType();

    class PrimitiveProtocolValueAdapter implements JsonDeserializer<PrimitiveProtocolValue<?>> {
        @Override
        public PrimitiveProtocolValue<?> deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in PrimitiveProtocolValue JSON");
            }

            String valueType = jsonObject.get("type").getAsString();

            switch (valueType) {
                case "undefined":
                    return jsonDeserializationContext.deserialize(jsonObject, UndefinedValue.class);
                case "null":
                    return jsonDeserializationContext.deserialize(jsonObject, NullValue.class);
                case "string":
                    return jsonDeserializationContext.deserialize(jsonObject, StringValue.class);
                case "number":
                    return jsonDeserializationContext.deserialize(jsonObject, NumberValue.class);
                case "boolean":
                    return jsonDeserializationContext.deserialize(jsonObject, BooleanValue.class);
                case "bigint":
                    return jsonDeserializationContext.deserialize(jsonObject, BigIntValue.class);
                default:
                    throw new JsonParseException("Unknown PrimitiveProtocolValue type: " + valueType);
            }
        }
    }

    class UndefinedValue implements PrimitiveProtocolValue<Void> {
        private final Type type = Type.UNDEFINED;

        @Override
        public Type getType() {
            return type;
        }
    }

    class NullValue implements PrimitiveProtocolValue<Void> {
        private final Type type = Type.NULL;

        @Override
        public Type getType() {
            return type;
        }
    }

    class StringValue implements PrimitiveProtocolValue<String> {
        private final Type type = Type.STRING;
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class NumberValue implements PrimitiveProtocolValue<String> {
        private final Type type = Type.NUMBER;
        private final String value; // Cannot use Number (as defined) because of special values

        public NumberValue(String value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class BooleanValue implements PrimitiveProtocolValue<Boolean> {
        private final Type type = Type.BOOLEAN;
        private final Boolean value;

        public BooleanValue(Boolean value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        public Boolean getValue() {
            return value;
        }
    }

    class BigIntValue implements PrimitiveProtocolValue<String> {
        private final Type type = Type.BIGINT;
        private final String value;

        public BigIntValue(String value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    enum Type implements EnumWrapper {
        UNDEFINED("undefined"),
        NULL("null"),
        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        BIGINT("bigint");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }

    enum SpecialNumber implements EnumWrapper {
        NAN("NaN"),
        NEGATIVE_ZERO("-0"),
        INFINITY("Infinity"),
        NEGATIVE_INFINITY("-Infinity");

        private final String value;

        SpecialNumber(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}
