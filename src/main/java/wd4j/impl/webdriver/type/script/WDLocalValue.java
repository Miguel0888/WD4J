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
                    return context.deserialize(jsonObject, ArrayWDLocalValue.class);
                case "date":
                    return context.deserialize(jsonObject, DateWDLocalValue.class);
                case "map":
                    return context.deserialize(jsonObject, MapWDLocalValue.class);
                case "object":
                    return context.deserialize(jsonObject, ObjectWDLocalValue.class);
                case "regexp":
                    return context.deserialize(jsonObject, RegExpWDLocalValue.class);
                case "set":
                    return context.deserialize(jsonObject, SetWDLocalValue.class);
                default:
                    throw new JsonParseException("Unknown LocalValue type: " + type);
            }
        }
    }

    class ArrayWDLocalValue implements WDLocalValue<List<WDLocalValue<?>>> {
        private final String type = "array";
        private final List<WDLocalValue<?>> value;

        public ArrayWDLocalValue(List<WDLocalValue<?>> value) {
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

    class DateWDLocalValue implements WDLocalValue<String> {
        private final String type = "date";
        private final String value;

        public DateWDLocalValue(String value) {
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

    class MapWDLocalValue implements WDLocalValue<Map<WDLocalValue<?>, WDLocalValue<?>>> {
        private final String type = "map";
        private final Map<WDLocalValue<?>, WDLocalValue<?>> value;

        public MapWDLocalValue(Map<WDLocalValue<?>, WDLocalValue<?>> value) {
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

    class ObjectWDLocalValue implements WDLocalValue<Map<WDLocalValue<?>, WDLocalValue<?>>> {
        private final String type = "object";
        private final Map<WDLocalValue<?>, WDLocalValue<?>> value;

        public ObjectWDLocalValue(Map<WDLocalValue<?>, WDLocalValue<?>> value) {
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

    class RegExpValue {
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

    class RegExpWDLocalValue implements WDLocalValue<RegExpValue> {
        private final String type = "regexp";
        private final RegExpValue value;

        public RegExpWDLocalValue(RegExpValue value) {
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
    }

    class SetWDLocalValue implements WDLocalValue<List<WDLocalValue<?>>> {
        private final String type = "set";
        private final List<WDLocalValue<?>> value;

        public SetWDLocalValue(List<WDLocalValue<?>> value) {
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