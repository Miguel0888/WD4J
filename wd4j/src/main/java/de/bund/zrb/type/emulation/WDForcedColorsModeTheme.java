package de.bund.zrb.type.emulation;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * Represent the WebDriver BiDi emulation.ForcedColorsModeTheme.
 */
public enum WDForcedColorsModeTheme implements EnumWrapper {
    LIGHT("light"),
    DARK("dark");

    private final String value;

    WDForcedColorsModeTheme(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
