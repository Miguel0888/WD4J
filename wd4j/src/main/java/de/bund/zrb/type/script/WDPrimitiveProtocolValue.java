package de.bund.zrb.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.support.mapping.EnumWrapper;

@JsonAdapter(WDPrimitiveProtocolValue.PrimitiveProtocolValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDPrimitiveProtocolValue extends WDLocalValue, WDRemoteValue {
    String getType();

    class PrimitiveProtocolValueAdapter implements JsonDeserializer<WDPrimitiveProtocolValue> {
        @Override
        public WDPrimitiveProtocolValue deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
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

    class UndefinedValue implements WDPrimitiveProtocolValue {
        private final String type = Type.UNDEFINED.value();

        @Override
        public String getType() {
            return type;
        }
    }

    class NullValue implements WDPrimitiveProtocolValue {
        private final String type = Type.NULL.value();

        @Override
        public String getType() {
            return type;
        }
    }

    class StringValue implements WDPrimitiveProtocolValue {
        private final String type = Type.STRING.value();
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class BooleanValue implements WDPrimitiveProtocolValue {
        private final String type = Type.BOOLEAN.value();
        private final Boolean value;

        public BooleanValue(Boolean value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public Boolean getValue() {
            return value;
        }
    }

    class BigIntValue implements WDPrimitiveProtocolValue {
        private final String type = Type.BIGINT.value();
        private final String value;

        public BigIntValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    // ToDo: EnumWrapper might be removed, since enum cannot be used as directly, instead String is used
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

    class NumberValue implements WDPrimitiveProtocolValue {
        private final String type = Type.NUMBER.value();
        private final String value;

        public NumberValue(String value) {
            if (!isValidNumber(value)) {
                throw new IllegalArgumentException("Invalid number value: " + value);
            }
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        /**
         * 🔥 Konvertiert den Wert in `Double`, falls es sich um eine echte Zahl handelt
         */
//        @Override
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
