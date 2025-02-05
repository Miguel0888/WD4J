package wd4j.impl.webdriver.type.script;

import java.util.List;

public class StackTrace {
    private final List<StackFrame> frames;

    public StackTrace(List<StackFrame> frames) {
        this.frames = frames;
    }

    public List<StackFrame> getFrames() {
        return frames;
    }
}