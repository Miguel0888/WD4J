package wd4j.impl.webdriver.command.response.session;

import wd4j.impl.markerInterfaces.resultData.SessionResult;

public class StatusResult implements SessionResult {
    private final boolean ready;
    private final String message;

    public StatusResult(boolean ready, String message) {
        this.ready = ready;
        this.message = message;
    }

    public boolean isReady() {
        return ready;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SessionStatusResult{" +
                "ready=" + ready +
                ", message='" + message + '\'' +
                '}';
    }
}
