package wd4j.impl.dto.type.script;

import wd4j.impl.dto.mapping.StringWrapper;

/**
 * The script.SharedId type represents a reference to a DOM Node that is usable in any realm (including Sandbox Realms).
 */
public class WDSharedId implements StringWrapper {
    public final String value;

    public WDSharedId(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
