package de.bund.zrb.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The script.LocalValue type represents values which can be deserialized into ECMAScript. This includes both primitive
 * and non-primitive values as well as remote references and channels.
 *
 */
@JsonAdapter(WDLocalValue.LocalValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDLocalValue {

    /**
     * Converts an object to a WDLocalValue. The object may be a primitive value, a remote reference, or a collection of
     * these.
     * @param arg The object to convert
     * @return
     */
    static WDLocalValue fromObject(Object arg) {
        if (arg == null) {
            return new WDPrimitiveProtocolValue.NullValue();

        } else if (arg instanceof String) {
            return new WDPrimitiveProtocolValue.StringValue((String) arg);

        } else if (arg instanceof Boolean) {
            return new WDPrimitiveProtocolValue.BooleanValue((Boolean) arg);

        } else if (arg instanceof Integer || arg instanceof Long || arg instanceof Float || arg instanceof Double) {
            return new WDPrimitiveProtocolValue.NumberValue(arg.toString());

        } else if (arg instanceof BigInteger) {
            return new WDPrimitiveProtocolValue.BigIntValue(arg.toString());

        } else if (arg instanceof WDRemoteReference.SharedReference || arg instanceof WDRemoteReference.RemoteObjectReference) {
            return (WDLocalValue) arg; // bereits korrekt

        } else if (arg instanceof List<?>) {
            List<?> list = (List<?>) arg;
            List<WDLocalValue> converted = list.stream()
                    .map(WDLocalValue::fromObject)
                    .collect(Collectors.toList());
            return new WDLocalValue.ArrayLocalValue(converted);

        } else if (arg instanceof Set<?>) {
            List<WDLocalValue> converted = ((Set<?>) arg).stream()
                    .map(WDLocalValue::fromObject)
                    .collect(Collectors.toList());
            return new WDLocalValue.SetLocalValue(converted);

        } else if (arg instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) arg;
            Map<WDLocalValue, WDLocalValue> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                WDLocalValue key = fromObject(entry.getKey());
                WDLocalValue value = fromObject(entry.getValue());
                converted.put(key, value);
            }
            return new WDLocalValue.MapLocalValue(converted);

        } else if (arg instanceof Date) {
            // ISO 8601-Format, wie es BiDi erwartet
            return new WDLocalValue.DateLocalValue(((Date) arg).toInstant().toString());
        }

        throw new IllegalArgumentException("Unsupported argument type: " + arg.getClass().getName());
    }


    String getType();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LocalValueAdapter implements JsonDeserializer<WDLocalValue>, JsonSerializer<WDLocalValue>  {
        @Override
        public WDLocalValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in LocalValue JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "array":
                    return context.deserialize(jsonObject, ArrayLocalValue.class);
                case "date":
                    return context.deserialize(jsonObject, DateLocalValue.class);
                case "map":
                    return context.deserialize(jsonObject, MapLocalValue.class);
                case "object":
                    return context.deserialize(jsonObject, ObjectLocalValue.class);
                case "regexp":
                    return context.deserialize(jsonObject, RegExpLocalValue.class);
                case "set":
                    return context.deserialize(jsonObject, SetLocalValue.class);

                // Further different types of LocalValue, not implemented in this class
                case "shared-reference":
                    return context.deserialize(jsonObject, WDRemoteReference.SharedReference.class);
                case "remote-object-reference":
                    return context.deserialize(jsonObject, WDRemoteReference.RemoteObjectReference.class);

                case "undefined":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.UndefinedValue.class);
                case "null":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.NullValue.class);
                case "string":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.StringValue.class);
                case "number":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.NumberValue.class);
                case "boolean":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.BooleanValue.class);
                case "bigint":
                    return context.deserialize(jsonObject, WDPrimitiveProtocolValue.BigIntValue.class);

                default:
                    throw new JsonParseException("Unknown LocalValue type: " + type);
            }
        }

        @Override
        public JsonElement serialize(WDLocalValue src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    class ArrayLocalValue implements WDLocalValue {
        private final String type = "array";
        private final List<WDLocalValue> value;

        public ArrayLocalValue(List<WDLocalValue> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public List<WDLocalValue> getValue() {
            return value;
        }
    }

    class DateLocalValue implements WDLocalValue {
        private final String type = "date";
        private final String value;

        public DateLocalValue(String value) {
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

    class MapLocalValue implements WDLocalValue {
        private final String type = "map";
        private final Map<WDLocalValue, WDLocalValue> value;

        public MapLocalValue(Map<WDLocalValue, WDLocalValue> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public Map<WDLocalValue, WDLocalValue> getValue() {
            return value;
        }
    }

    class ObjectLocalValue<T> implements WDLocalValue {
        private final String type = "object";
        private final Map<T, WDLocalValue> value; // T may be String or WDLocalValue

        public ObjectLocalValue(Map<T, WDLocalValue> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public Map<T, WDLocalValue> getValue() {
            return value;
        }
    }

    class RegExpLocalValue implements WDLocalValue {
        private final String type = "regexp";
        private final RegExpValue value;

        public RegExpLocalValue(RegExpValue value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public RegExpValue getValue() {
            return value;
        }

        public static class RegExpValue {
            private final String pattern;
            private final String flags; // Optional

            public RegExpValue(String pattern) {
                this(pattern, null);
            }

            public RegExpValue(String pattern, String flags) {
                this.pattern = pattern;
                this.flags = flags;
            }

            public String getPattern() {
                return pattern;
            }

            public String getFlags() {
                return flags;
            }
        }
    }

    class SetLocalValue implements WDLocalValue {
        private final String type = "set";
        private final List<WDLocalValue> value;

        public SetLocalValue(List<WDLocalValue> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public List<WDLocalValue> getValue() {
            return value;
        }
    }
}