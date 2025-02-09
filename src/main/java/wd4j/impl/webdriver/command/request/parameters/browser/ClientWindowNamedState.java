package wd4j.impl.webdriver.command.request.parameters.browser;

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
