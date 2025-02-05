package wd4j.impl.webdriver.type.log.logEntry;

import wd4j.impl.webdriver.type.log.Level;
import wd4j.impl.webdriver.type.log.LogEntry;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.webdriver.type.script.StackTrace;

public class BaseLogEntry implements LogEntry {
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
