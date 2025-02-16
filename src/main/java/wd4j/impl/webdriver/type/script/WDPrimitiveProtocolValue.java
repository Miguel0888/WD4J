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

    /**
     * 🔥 Konvertiert PrimitiveProtocolValue in einen String
     */
    default String asString() {
        switch (getType()) {
            case STRING:
                return ((StringValue) this).getValue();
            case NUMBER:
                return ((NumberValue) this).getValue();
            case BOOLEAN:
                return Boolean.toString(((BooleanValue) this).getValue());
            case BIGINT:
                return ((BigIntValue) this).getValue();
            case NULL:
                return "null";
            case UNDEFINED:
                return "undefined";
            default:
                throw new UnsupportedOperationException("Cannot convert type " + getType() + " to String.");
        }
    }

    /**
     * 🔥 Gibt den Wert in seinem nativen Typ zurück (String, Number, Boolean, etc.)
     */
    default Object asObject() {
        switch (getType()) {
            case STRING:
                return ((StringValue) this).getValue();
            case NUMBER:
                return Double.valueOf(((NumberValue) this).getValue());
            case BOOLEAN:
                return ((BooleanValue) this).getValue();
            case BIGINT:
                return new java.math.BigInteger(((BigIntValue) this).getValue());
            case NULL:
                return null;
            case UNDEFINED:
                return null; // Kann `undefined` nicht direkt als Java-Typ repräsentieren
            default:
                throw new UnsupportedOperationException("Cannot convert type " + getType() + " to Object.");
        }
    }

    class UndefinedValue implements WDPrimitiveProtocolValue<Void> {
        private final Type type = Type.UNDEFINED;

        @Override
        public Type getType() {
            return type;
        }
    }

    class NullValue implements WDPrimitiveProtocolValue<Void> {
        private final Type type = Type.NULL;

        @Override
        public Type getType() {
            return type;
        }
    }

    class StringValue implements WDPrimitiveProtocolValue<String> {
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

    class BooleanValue implements WDPrimitiveProtocolValue<Boolean> {
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

    class BigIntValue implements WDPrimitiveProtocolValue<String> {
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

    class NumberValue implements WDPrimitiveProtocolValue<String> {
        private final Type type = Type.NUMBER;
        private final String value;

        public NumberValue(String value) {
            if (!isValidNumber(value)) {
                throw new IllegalArgumentException("Invalid number value: " + value);
            }
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        /**
         * 🔥 Konvertiert den Wert in `Double`, falls es sich um eine echte Zahl handelt
         */
        @Override
        public Object asObject() {
            if (EnumWrapper.contains(SpecialNumber.class, value)) {
                return value; // SpecialNumber bleibt als String
            }
            return Double.valueOf(value);
        }

        /**
         * 🔥 Prüft, ob der Wert eine gültige Zahl oder ein `SpecialNumber` ist.
         */
        private static boolean isValidNumber(String value) {
            return EnumWrapper.contains(SpecialNumber.class, value) || isNumeric(value);
        }

        /**
         * 🔥 Hilfsmethode zur Prüfung, ob ein String eine echte Zahl ist.
         */
        private static boolean isNumeric(String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
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
