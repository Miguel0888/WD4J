package wd4j.impl.webdriver.command.request.network.parameters;

public enum InterceptPhase {
    BEFORE_REQUEST_SENT("beforeRequestSent"),
    RESPONSE_STARTED("responseStarted"),
    AUTH_REQUIRED("authRequired");

    private final String phase;

    InterceptPhase(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }
}
