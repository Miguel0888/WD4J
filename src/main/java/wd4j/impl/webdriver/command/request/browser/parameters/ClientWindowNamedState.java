package wd4j.impl.webdriver.command.request.browser.parameters;

public enum ClientWindowNamedState implements ClientWindowState{
    FULLSCREEN("fullscreen"),
    MAXIMIZED("maximized"),
    MINIMIZED("minimized");

    private final String state;

    private ClientWindowNamedState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
