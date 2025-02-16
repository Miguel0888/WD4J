package wd4j.impl.webdriver.type.script;

import java.util.List;

public class WDStackTrace {
    private final List<WDStackFrame> frames;

    public WDStackTrace(List<WDStackFrame> frames) {
        this.frames = frames;
    }

    public List<WDStackFrame> getFrames() {
        return frames;
    }
}