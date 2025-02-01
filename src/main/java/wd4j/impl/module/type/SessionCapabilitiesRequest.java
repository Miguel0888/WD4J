package wd4j.impl.module.type;

import java.util.Map;

public class SessionCapabilitiesRequest {
    private final Map<String, Object> capabilities;

    public SessionCapabilitiesRequest(Map<String, Object> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            throw new IllegalArgumentException("Capabilities must not be null or empty.");
        }
        this.capabilities = capabilities;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }
}