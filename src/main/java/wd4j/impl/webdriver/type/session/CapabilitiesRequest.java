package wd4j.impl.webdriver.type.session;

import java.util.List;

public class CapabilitiesRequest {
    private final CapabilityRequest alwaysMatch; // Optional
    private final List<CapabilityRequest> firstMatch; // Optional

    public CapabilitiesRequest(CapabilityRequest alwaysMatch, List<CapabilityRequest> firstMatch) {
        this.alwaysMatch = alwaysMatch;
        this.firstMatch = firstMatch;
    }

    public CapabilityRequest getAlwaysMatch() {
        return alwaysMatch;
    }

    public List<CapabilityRequest> getFirstMatch() {
        return firstMatch;
    }
}
