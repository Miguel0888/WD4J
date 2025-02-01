package wd4j.impl.module.type;

public class BrowsingContextReadinessState {
    private final String state;

    public BrowsingContextReadinessState(String state) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("State must not be null or empty.");
        }
        this.state = state;
    }

    public String getState() {
        return state;
    }
}