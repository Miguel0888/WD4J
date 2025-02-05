package wd4j.impl.webdriver.type.log.logEntry;

import wd4j.impl.webdriver.type.log.Level;
import wd4j.impl.webdriver.type.log.LogEntry;
import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.webdriver.type.script.StackTrace;

import java.util.List;

public class ConsoleLogEntry extends BaseLogEntry {
    private final String type = "console";
    private String method;
    private List<RemoteValue> args;

    public ConsoleLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace, String method, List<RemoteValue> args) {
        super(level, source, text, timestamp, stackTrace);
        this.method = method;
        this.args = args;
    }
}
