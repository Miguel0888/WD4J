package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * Represent the WebDriver BiDi network.CollectorType.
 * Currently only "blob" is defined by the spec.
 */
public enum WDCollectorType implements EnumWrapper {
    BLOB("blob");

    private final String value;

    WDCollectorType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
