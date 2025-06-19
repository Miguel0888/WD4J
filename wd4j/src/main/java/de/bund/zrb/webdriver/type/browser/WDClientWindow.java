package de.bund.zrb.webdriver.type.browser;

import de.bund.zrb.markerInterfaces.WDType;
import de.bund.zrb.webdriver.mapping.StringWrapper;

public class WDClientWindow implements WDType<WDClientWindow>, StringWrapper {
    private final String value;

    public WDClientWindow(String value) {
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