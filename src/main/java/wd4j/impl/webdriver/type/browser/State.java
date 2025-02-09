package wd4j.impl.webdriver.type.browser;

// ToDo: See ClientWindowNamedState.java & ClientWindowRectState.java
public enum State {
    FULLSCREEN ("fullscreen"),
    MAXIMIZED ("maximized"),
    MINIMIZED ("minimized"),
    NORMAL ("normal");

    private final String value;

    State(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}