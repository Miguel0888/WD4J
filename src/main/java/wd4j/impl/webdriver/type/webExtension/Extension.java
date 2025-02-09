package wd4j.impl.webdriver.type.webExtension;

import wd4j.impl.markerInterfaces.Type;
import wd4j.impl.webdriver.mapping.StringWrapper;

public class Extension implements StringWrapper {
    private final String value;

    public Extension(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Extension must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
