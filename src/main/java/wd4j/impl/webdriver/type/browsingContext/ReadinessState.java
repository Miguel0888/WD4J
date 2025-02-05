package wd4j.impl.webdriver.type.browsingContext;

public class ReadinessState {
    private final String state;

    public ReadinessState(String state) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("State must not be null or empty.");
        }
        this.state = state;
    }

    public String getState() {
        return state;
    }
}