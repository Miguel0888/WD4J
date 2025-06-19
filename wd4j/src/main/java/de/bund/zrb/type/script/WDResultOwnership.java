package de.bund.zrb.type.script;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * The script.ResultOwnership specifies how the serialized value ownership will be treated.
 */
public enum WDResultOwnership implements EnumWrapper {
    ROOT("root"),
    NONE("none");

    private final String value;

    WDResultOwnership(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
