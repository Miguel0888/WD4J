package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.webdriver.mapping.StringWrapper;

public class WDBrowsingContext implements WDType<WDBrowsingContext>, StringWrapper {
    private final String value;

    public WDBrowsingContext(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
