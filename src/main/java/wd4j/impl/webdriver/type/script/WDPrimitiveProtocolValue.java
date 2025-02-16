package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;

@JsonAdapter(WDPrimitiveProtocolValue.PrimitiveProtocolValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDPrimitiveProtocolValue<T> {
    Type getType();

    class PrimitiveProtocolValueAdapter implements JsonDeserializer<WDPrimitiveProtocolValue<?>> {
        @Override
        public WDPrimitiveProtocolValue<?> deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in PrimitiveProtocolValue JSON");
            }

            String valueType = jsonObject.get("type").getAsString();

            switch (valueType) {
                case "undefined":
                    return jsonDeserializationContext.deserialize(jsonObject, UndefinedValueWD.class);
                case "null":
                    return jsonDeserializationContext.deserialize(jsonObject, NullValueWD.class);
                case "string":
                    return jsonDeserializationContext.deserialize(jsonObject, StringValueWD.class);
                case "number":
                    return jsonDeserializationContext.deserialize(jsonObject, NumberValueWD.class);
                case "boolean":
                    return jsonDeserializationContext.deserialize(jsonObject, BooleanValueWD.class);
                case "bigint":
                    return jsonDeserializationContext.deserialize(jsonObject, BigIntValueWD.class);
                default:
                    throw new JsonParseException("Unknown PrimitiveProtocolValue type: " + valueType);
            }
        }
    }

    class UndefinedValueWD implements WDPrimitiveProtocolValue<Void> {
        private final Type type = Type.UNDEFINED;

        @Override
        public Type getType() {
            return type;
        }
    }

    class NullValueWD implements WDPrimitiveProtocolValue<Void> {
        private final Type type = Type.NULL;

        @Override
        public Type getType() {
            return type;
        }
    }

    class StringValueWD implements WDPrimitiveProtocolValue<String> {
        private final Type type = Type.STRING;
        private final String value;

        public StringValueWD(String value) {
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

    class NumberValueWD implements WDPrimitiveProtocolValue<String> {
        private final Type type = Type.NUMBER;
        private final String value; // Cannot use Number (as defined) because of special values

        public NumberValueWD(String value) {
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

    class BooleanValueWD implements WDPrimitiveProtocolValue<Boolean> {
        private final Type type = Type.BOOLEAN;
        private final Boolean value;

        public BooleanValueWD(Boolean value) {
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

    class BigIntValueWD implements WDPrimitiveProtocolValue<String> {
        private final Type type = Type.BIGINT;
        private final String value;

        public BigIntValueWD(String value) {
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
