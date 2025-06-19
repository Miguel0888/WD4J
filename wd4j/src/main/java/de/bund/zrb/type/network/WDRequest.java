package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.StringWrapper;

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