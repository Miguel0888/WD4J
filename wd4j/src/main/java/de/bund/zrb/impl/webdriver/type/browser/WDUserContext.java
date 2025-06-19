package de.bund.zrb.impl.webdriver.type.browser;

import de.bund.zrb.impl.markerInterfaces.WDType;
import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

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