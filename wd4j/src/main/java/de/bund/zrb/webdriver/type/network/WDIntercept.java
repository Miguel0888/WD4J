package de.bund.zrb.webdriver.type.network;

import de.bund.zrb.webdriver.mapping.StringWrapper;

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