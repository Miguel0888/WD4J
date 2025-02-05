package wd4j.impl.webdriver.type.browsingContext;

public class UserPromptType {
    private final String type;

    public UserPromptType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
    }

    public String getType() {
        return type;
    }
}