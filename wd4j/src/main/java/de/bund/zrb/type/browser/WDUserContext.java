package de.bund.zrb.type.browser;

import de.bund.zrb.api.markerInterfaces.WDType;
import de.bund.zrb.support.mapping.StringWrapper;

public class WDUserContext implements WDType<WDUserContext>, StringWrapper {
    private final String value;

    public WDUserContext(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}