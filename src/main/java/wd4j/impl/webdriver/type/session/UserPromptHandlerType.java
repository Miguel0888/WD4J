package wd4j.impl.webdriver.type.session;

public class UserPromptHandlerType {
    private final String type;

    public UserPromptHandlerType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
    }

    public String getType() {
        return type;
    }
}