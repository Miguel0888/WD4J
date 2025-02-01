package wd4j.impl.module.type;

public class SessionUserPromptHandlerType {
    private final String type;

    public SessionUserPromptHandlerType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
    }

    public String getType() {
        return type;
    }
}