package wd4j.impl.modules;

import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Module;

import java.util.List;
import java.util.Map;

public class LogService implements Module {

    private final WebSocketConnection webSocketConnection;

    public LogService(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class LogEntry {

        private final String level;
        private final String source;
        private final String text;
        private final String timestamp;
        private final List<Map<String, Object>> stackTrace;
        private final Map<String, Object> args;

        /**
         * Constructor for LogEntry.
         *
         * @param level      The severity level of the log (e.g., "info", "warning", "error").
         * @param source     The source of the log (e.g., "console", "network").
         * @param text       The text of the log entry.
         * @param timestamp  The timestamp when the log entry was created.
         * @param stackTrace Optional stack trace information.
         * @param args       Optional additional arguments.
         */
        public LogEntry(String level, String source, String text, String timestamp,
                        List<Map<String, Object>> stackTrace, Map<String, Object> args) {
            if (level == null || level.isEmpty()) {
                throw new IllegalArgumentException("Log level must not be null or empty.");
            }
            if (source == null || source.isEmpty()) {
                throw new IllegalArgumentException("Log source must not be null or empty.");
            }
            if (text == null || text.isEmpty()) {
                throw new IllegalArgumentException("Log text must not be null or empty.");
            }
            if (timestamp == null || timestamp.isEmpty()) {
                throw new IllegalArgumentException("Log timestamp must not be null or empty.");
            }

            this.level = level;
            this.source = source;
            this.text = text;
            this.timestamp = timestamp;
            this.stackTrace = stackTrace;
            this.args = args;
        }

        // Getters
        public String getLevel() {
            return level;
        }

        public String getSource() {
            return source;
        }

        public String getText() {
            return text;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public List<Map<String, Object>> getStackTrace() {
            return stackTrace;
        }

        public Map<String, Object> getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return "LogEntry{" +
                    "level='" + level + '\'' +
                    ", source='" + source + '\'' +
                    ", text='" + text + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", stackTrace=" + stackTrace +
                    ", args=" + args +
                    '}';
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}