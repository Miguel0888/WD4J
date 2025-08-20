package de.bund.zrb.type.emulation;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * Represent the orientation type as per spec.
 */
public enum WDScreenOrientationType implements EnumWrapper {
    PORTRAIT_PRIMARY("portrait-primary"),
    PORTRAIT_SECONDARY("portrait-secondary"),
    LANDSCAPE_PRIMARY("landscape-primary"),
    LANDSCAPE_SECONDARY("landscape-secondary");

    private final String value;

    WDScreenOrientationType(String value) { this.value = value; }

    @Override
    public String value() { return value; }
}