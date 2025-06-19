package de.bund.zrb.impl.webdriver.type.network;

import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

public class WDIntercept implements StringWrapper {
    private final String value;

    public WDIntercept(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}