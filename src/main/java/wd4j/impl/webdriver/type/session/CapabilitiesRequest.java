package wd4j.impl.webdriver.type.session;

import java.util.Map;

public class CapabilitiesRequest {
    private final Map<String, Object> capabilities;

    public CapabilitiesRequest(Map<String, Object> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            throw new IllegalArgumentException("Capabilities must not be null or empty.");
        }
        this.capabilities = capabilities;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }
}