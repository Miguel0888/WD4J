package de.bund.zrb.type.browsingContext;

import de.bund.zrb.support.mapping.StringWrapper;

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