package de.bund.zrb.type.session;

import java.util.List;

/**
 * The session.CapabilitiesRequest type represents the capabilities requested for a session.
 */
public class WDCapabilitiesRequest {
    private final WDCapabilityRequest alwaysMatch; // Optional
    private final List<WDCapabilityRequest> firstMatch; // Optional

    public WDCapabilitiesRequest() {
        this(null, null);
    }

    public WDCapabilitiesRequest(WDCapabilityRequest alwaysMatch) {
        this(alwaysMatch, null);
    }

    public WDCapabilitiesRequest(WDCapabilityRequest alwaysMatch, List<WDCapabilityRequest> firstMatch) {
        this.alwaysMatch = alwaysMatch;
        this.firstMatch = firstMatch;
    }

    public WDCapabilityRequest getAlwaysMatch() {
        return alwaysMatch;
    }

    public List<WDCapabilityRequest> getFirstMatch() {
        return firstMatch;
    }
}
