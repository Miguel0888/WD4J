package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@JsonAdapter(WDLocalValue.LocalValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDLocalValue<T> {
    String getType();
    T getValue();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LocalValueAdapter implements JsonDeserializer<WDLocalValue<?>> {
        @Override
        public WDLocalValue<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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
                default:
                    throw new JsonParseException("Unknown LocalValue type: " + type);
            }
        }
    }

    class ArrayLocalValue implements WDLocalValue<List<WDLocalValue<?>>> {
        private final String type = "array";
        private final List<WDLocalValue<?>> value;

        public ArrayLocalValue(List<WDLocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public List<WDLocalValue<?>> getValue() {
            return value;
        }
    }

    class DateLocalValue implements WDLocalValue<String> {
        private final String type = "date";
        private final String value;

        public DateLocalValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    class MapLocalValue implements WDLocalValue<Map<WDLocalValue<?>, WDLocalValue<?>>> {
        private final String type = "map";
        private final Map<WDLocalValue<?>, WDLocalValue<?>> value;

        public MapLocalValue(Map<WDLocalValue<?>, WDLocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Map<WDLocalValue<?>, WDLocalValue<?>> getValue() {
            return value;
        }
    }

    class ObjectLocalValue implements WDLocalValue<Map<WDLocalValue<?>, WDLocalValue<?>>> {
        private final String type = "object";
        private final Map<WDLocalValue<?>, WDLocalValue<?>> value;

        public ObjectLocalValue(Map<WDLocalValue<?>, WDLocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Map<WDLocalValue<?>, WDLocalValue<?>> getValue() {
            return value;
        }
    }

    class RegExpLocalValue implements WDLocalValue<RegExpLocalValue.RegExpValue> {
        private final String type = "regexp";
        private final RegExpValue value;

        public RegExpLocalValue(RegExpValue value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
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

    class SetLocalValue implements WDLocalValue<List<WDLocalValue<?>>> {
        private final String type = "set";
        private final List<WDLocalValue<?>> value;

        public SetLocalValue(List<WDLocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public List<WDLocalValue<?>> getValue() {
            return value;
        }
    }
}