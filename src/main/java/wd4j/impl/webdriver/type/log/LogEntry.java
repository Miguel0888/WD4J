package wd4j.impl.webdriver.type.log;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.webdriver.type.script.StackTrace;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(LogEntry.LogEntryAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface LogEntry {

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LogEntryAdapter implements JsonDeserializer<LogEntry> {
        @Override
        public LogEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in LogEntry JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "console":
                    return context.deserialize(jsonObject, ConsoleLogEntry.class);
                case "javascript":
                    return context.deserialize(jsonObject, JavascriptLogEntry.class);
                default:
                    return context.deserialize(jsonObject, GenericLogEntry.class);
            }
        }
    }

    class BaseLogEntry implements LogEntry {
        private final Level level;
        private final Source source;
        private final String text;
        private final long timestamp;
        private final StackTrace stackTrace;

        public BaseLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace) {
            this.level = level;
            this.source = source;
            this.text = text;
            this.timestamp = timestamp;
            this.stackTrace = stackTrace;
        }

        public BaseLogEntry(Level level, Source source, String text, long timestamp) {
            this(level, source, text, timestamp, null);
        }

        public Level getLevel() {
            return level;
        }

        public Source getSource() {
            return source;
        }

        public String getText() {
            return text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public StackTrace getStackTrace() {
            return stackTrace;
        }
    }

    class ConsoleLogEntry extends BaseLogEntry {
        private final String type = "console";
        private String method;
        private List<RemoteValue> args;

        public ConsoleLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace, String method, List<RemoteValue> args) {
            super(level, source, text, timestamp, stackTrace);
            this.method = method;
            this.args = args;
        }
    }

    class GenericLogEntry extends BaseLogEntry {
        private final String type;

        public GenericLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace, String type) {
            super(level, source, text, timestamp, stackTrace);
            this.type = type;
        }
    }

    class JavascriptLogEntry extends BaseLogEntry {
        private final String type = "javascript";

        public JavascriptLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace) {
            super(level, source, text, timestamp, stackTrace);
        }
    }
}