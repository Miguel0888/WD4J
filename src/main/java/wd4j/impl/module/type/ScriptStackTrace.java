package wd4j.impl.module.type;

import java.util.List;

public class ScriptStackTrace {
    private final List<ScriptStackFrame> frames;

    public ScriptStackTrace(List<ScriptStackFrame> frames) {
        this.frames = frames;
    }

    public List<ScriptStackFrame> getFrames() {
        return frames;
    }
}