package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.StringWrapper;

/**
 * Each realm has an associated realm id, which is a string uniquely identifying that realm. This is implicitly set when
 * the realm is created.
 *
 * The realm id for a realm is opaque and must not be derivable from the handle id of the corresponding global object in
 * the handle object map or, where relevant, from the navigable id of any navigable.
 */
public class Realm implements StringWrapper {
    private final String value;

    public Realm(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}