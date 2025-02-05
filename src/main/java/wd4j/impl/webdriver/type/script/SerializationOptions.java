package wd4j.impl.webdriver.type.script;

public class SerializationOptions {
    private final boolean maxDepth;

    public SerializationOptions(boolean maxDepth) {
        this.maxDepth = maxDepth;
    }

    public boolean isMaxDepth() {
        return maxDepth;
    }
}