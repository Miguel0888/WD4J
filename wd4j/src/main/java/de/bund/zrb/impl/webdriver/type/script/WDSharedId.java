package de.bund.zrb.impl.webdriver.type.script;

import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

import java.util.Objects;

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WDSharedId that = (WDSharedId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
