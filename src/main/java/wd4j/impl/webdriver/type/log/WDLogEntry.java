package wd4j.impl.webdriver.type.log;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.script.WDRemoteValue;
import wd4j.impl.webdriver.type.script.WDSource;
import wd4j.impl.webdriver.type.script.WDStackTrace;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(WDLogEntry.LogEntryAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDLogEntry {

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LogEntryAdapter implements JsonDeserializer<WDLogEntry> {
        @Override
        public WDLogEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in LogEntry JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "console":
                    return context.deserialize(jsonObject, ConsoleWDLogEntry.class);
                case "javascript":
                    return context.deserialize(jsonObject, JavascriptWDLogEntry.class);
                default:
                    return context.deserialize(jsonObject, GenericWDLogEntry.class);
            }
        }
    }

    class BaseWDLogEntry implements WDLogEntry {
        private final Level level;
        private final WDSource source;
        private final String text;
        private final long timestamp;
        private final WDStackTrace stackTrace;

        public BaseWDLogEntry(Level level, WDSource source, String text, long timestamp, WDStackTrace stackTrace) {
            this.level = level;
            this.source = source;
            this.text = text;
            this.timestamp = timestamp;
            this.stackTrace = stackTrace;
        }

        public BaseWDLogEntry(Level level, WDSource source, String text, long timestamp) {
            this(level, source, text, timestamp, null);
        }

        public Level getLevel() {
            return level;
        }

        public WDSource getSource() {
            return source;
        }

        public String getText() {
            return text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public WDStackTrace getStackTrace() {
            return stackTrace;
        }
    }

    class ConsoleWDLogEntry extends BaseWDLogEntry {
        private final String type = "console";
        private String method;
        private List<WDRemoteValue> args;

        public ConsoleWDLogEntry(Level Level, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace, String method, List<WDRemoteValue> args) {
            super(Level, WDSource, text, timestamp, WDStackTrace);
            this.method = method;
            this.args = args;
        }

        public String getMethod() {
            return method;
        }

        public List<WDRemoteValue> getArgs() {
            return args;
        }
    }

    class GenericWDLogEntry extends BaseWDLogEntry {
        private final String type;

        public GenericWDLogEntry(Level Level, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace, String type) {
            super(Level, WDSource, text, timestamp, WDStackTrace);
            this.type = type;
        }
    }

    class JavascriptWDLogEntry extends BaseWDLogEntry {
        private final String type = "javascript";

        public JavascriptWDLogEntry(Level Level, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace) {
            super(Level, WDSource, text, timestamp, WDStackTrace);
        }
    }

    enum Level implements EnumWrapper {
        DEBUG("debug"),
        INFO("info"),
        WARN("warn"),
        ERROR("error");

        private final String value;

        Level(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}