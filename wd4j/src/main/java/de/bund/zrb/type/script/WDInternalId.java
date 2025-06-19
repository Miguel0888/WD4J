package de.bund.zrb.type.script;

import de.bund.zrb.support.mapping.StringWrapper;

/**
 * The script.InternalId type represents the id of a previously serialized script.RemoteValue during serialization.
 */
public class WDInternalId implements StringWrapper {
    private final String value;

    public WDInternalId(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}