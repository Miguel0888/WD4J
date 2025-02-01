package wd4j.impl.module.type;

public class ScriptSerializationOptions {
    private final boolean maxDepth;

    public ScriptSerializationOptions(boolean maxDepth) {
        this.maxDepth = maxDepth;
    }

    public boolean isMaxDepth() {
        return maxDepth;
    }
}