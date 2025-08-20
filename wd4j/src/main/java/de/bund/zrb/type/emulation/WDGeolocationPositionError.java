package de.bund.zrb.type.emulation;

import de.bund.zrb.support.mapping.EnumWrapper;

/**
 * Represent the emulation.GeolocationPositionError.
 * Currently only "positionUnavailable" is defined.
 */
public enum WDGeolocationPositionError implements EnumWrapper {
    POSITION_UNAVAILABLE("positionUnavailable");

    private final String value;

    WDGeolocationPositionError(String value) { this.value = value; }

    @Override
    public String value() { return value; }
}