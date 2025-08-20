package de.bund.zrb.type.emulation;

/**
 * Represent the emulation.ScreenOrientation struct.
 */
public class WDScreenOrientation {
    private final WDScreenOrientationNatural natural;
    private final WDScreenOrientationType type;

    public WDScreenOrientation(WDScreenOrientationNatural natural, WDScreenOrientationType type) {
        if (natural == null) throw new IllegalArgumentException("natural must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        this.natural = natural;
        this.type = type;
    }

    public WDScreenOrientationNatural getNatural() { return natural; }
    public WDScreenOrientationType getType() { return type; }
}