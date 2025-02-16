package wd4j.impl.webdriver.type.log;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
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
        private final WDLevel WDLevel;
        private final WDSource WDSource;
        private final String text;
        private final long timestamp;
        private final WDStackTrace WDStackTrace;

        public BaseWDLogEntry(WDLevel WDLevel, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace) {
            this.WDLevel = WDLevel;
            this.WDSource = WDSource;
            this.text = text;
            this.timestamp = timestamp;
            this.WDStackTrace = WDStackTrace;
        }

        public BaseWDLogEntry(WDLevel WDLevel, WDSource WDSource, String text, long timestamp) {
            this(WDLevel, WDSource, text, timestamp, null);
        }

        public WDLevel getLevel() {
            return WDLevel;
        }

        public WDSource getSource() {
            return WDSource;
        }

        public String getText() {
            return text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public WDStackTrace getStackTrace() {
            return WDStackTrace;
        }
    }

    class ConsoleWDLogEntry extends BaseWDLogEntry {
        private final String type = "console";
        private String method;
        private List<WDRemoteValue> args;

        public ConsoleWDLogEntry(WDLevel WDLevel, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace, String method, List<WDRemoteValue> args) {
            super(WDLevel, WDSource, text, timestamp, WDStackTrace);
            this.method = method;
            this.args = args;
        }
    }

    class GenericWDLogEntry extends BaseWDLogEntry {
        private final String type;

        public GenericWDLogEntry(WDLevel WDLevel, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace, String type) {
            super(WDLevel, WDSource, text, timestamp, WDStackTrace);
            this.type = type;
        }
    }

    class JavascriptWDLogEntry extends BaseWDLogEntry {
        private final String type = "javascript";

        public JavascriptWDLogEntry(WDLevel WDLevel, WDSource WDSource, String text, long timestamp, WDStackTrace WDStackTrace) {
            super(WDLevel, WDSource, text, timestamp, WDStackTrace);
        }
    }
}