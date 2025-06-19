package de.bund.zrb.impl.webdriver.type.browsingContext;

import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

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