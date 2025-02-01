package wd4j.impl.module.type;

public class BrowsingContextUserPromptType {
    private final String type;

    public BrowsingContextUserPromptType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
    }

    public String getType() {
        return type;
    }
}