package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.StringWrapper;

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