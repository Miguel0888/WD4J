package wd4j.impl.dto.type.browser;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.dto.mapping.StringWrapper;

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