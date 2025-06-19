package de.bund.zrb.impl.webdriver.type.webExtension;

import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

public class WDExtension implements StringWrapper {
    private final String value;

    public WDExtension(String value) {
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
