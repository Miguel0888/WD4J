package wd4j.impl.webdriver.command.response.session;

import wd4j.impl.markerInterfaces.resultData.SessionResult;
import java.util.Map;

public class NewResult implements SessionResult {
    private final String sessionId;
    private final Map<String, Object> capabilities;

    public NewResult(String sessionId, Map<String, Object> capabilities) {
        this.sessionId = sessionId;
        this.capabilities = capabilities;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    @Override
    public String toString() {
        return "SessionNewResult{" +
                "sessionId='" + sessionId + '\'' +
                ", capabilities=" + capabilities +
                '}';
    }
}
