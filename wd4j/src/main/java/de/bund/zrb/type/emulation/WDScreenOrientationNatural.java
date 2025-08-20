package de.bund.zrb.type.emulation;

import de.bund.zrb.support.mapping.EnumWrapper;

/** Represent the natural orientation: "portrait" | "landscape". */
public enum WDScreenOrientationNatural implements EnumWrapper {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    private final String value;

    WDScreenOrientationNatural(String value) { this.value = value; }

    public String value() { return value; }
}
