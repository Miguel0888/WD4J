package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum InterceptPhase implements EnumWrapper {
    BEFORE_REQUEST_SENT("beforeRequestSent"),
    RESPONSE_STARTED("responseStarted"),
    AUTH_REQUIRED("authRequired");

    private final String value;

    InterceptPhase(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
