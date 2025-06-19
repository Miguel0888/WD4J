package de.bund.zrb.webdriver.type.network;

import de.bund.zrb.webdriver.mapping.StringWrapper;

public class WDRequest implements StringWrapper {
    private final String value;

    public WDRequest(String id) {
        this.value = id;
    }

    @Override // confirmed
    public String value() {
        return value;
    }

}