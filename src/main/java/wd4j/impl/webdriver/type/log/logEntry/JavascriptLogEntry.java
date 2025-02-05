package wd4j.impl.webdriver.type.log.logEntry;

import wd4j.impl.webdriver.type.log.Level;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.webdriver.type.script.StackTrace;

public class JavascriptLogEntry extends BaseLogEntry {
    private final String type = "javascript";

    public JavascriptLogEntry(Level level, Source source, String text, long timestamp, StackTrace stackTrace) {
        super(level, source, text, timestamp, stackTrace);
    }
}
