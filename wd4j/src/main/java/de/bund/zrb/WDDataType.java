package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * Represent the WebDriver BiDi network.DataType.
 * Currently the spec defines only "response".
 */
public enum WDDataType implements EnumWrapper {
    RESPONSE("response");

    private final String value;

    WDDataType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
