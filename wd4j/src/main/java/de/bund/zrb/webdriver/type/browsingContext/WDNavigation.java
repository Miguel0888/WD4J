package de.bund.zrb.webdriver.type.browsingContext;

import de.bund.zrb.webdriver.mapping.StringWrapper;

public class WDNavigation implements StringWrapper {
    private final String value;

    public WDNavigation(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}