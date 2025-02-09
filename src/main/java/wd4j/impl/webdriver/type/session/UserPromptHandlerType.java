package wd4j.impl.webdriver.type.session;

public enum UserPromptHandlerType {
    ACCEPT("accept"),
    DISMISS("dismiss"),
    IGNORE("ignore");

    private final String value;

    UserPromptHandlerType(String value) {
        this.value = value;
    }
}