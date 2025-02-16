package wd4j.impl.webdriver.type.browser;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.webdriver.mapping.StringWrapper;

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