package wd4j.impl.webdriver.type.log.logEntry;

import wd4j.impl.webdriver.type.log.Level;
import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.webdriver.type.script.StackTrace;

import java.util.List;

public class GenericLogEntry extends BaseLogEntry {
    private final String type;

    public GenericLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace, String type) {
        super(level, source, text, timestamp, stackTrace);
        this.type = type;
    }
}
