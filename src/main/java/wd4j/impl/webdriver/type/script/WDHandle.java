package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.StringWrapper;

import java.util.Objects;

/**
 * The script.Handle type represents a handle to an object owned by the ECMAScript runtime. The handle is only valid in a specific Realm.
 *
 * Each ECMAScript Realm has a corresponding handle object map. This is a strong map from handle ids to their corresponding objects.
 */
public class WDHandle implements StringWrapper {
    private final String value;

    public WDHandle(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Handle must not be null or empty.");
        }
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
        WDHandle handle = (WDHandle) o;
        return Objects.equals(value, handle.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}