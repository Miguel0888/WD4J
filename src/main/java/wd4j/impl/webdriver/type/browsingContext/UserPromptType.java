package wd4j.impl.webdriver.type.browsingContext;

public enum UserPromptType {
    ALERT("alert"),
    BEFOREUNLOAD("beforeunload"),
    CONFIRM("confirm"),
    PROMPT("prompt");

    private final String value;

    UserPromptType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}