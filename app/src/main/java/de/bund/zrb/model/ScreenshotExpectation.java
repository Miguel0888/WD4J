package de.bund.zrb.model;

import de.bund.zrb.model.ThenExpectation;

import java.util.Map;

/**
 * Hilfsklasse zum typsicheren Zugriff auf Screenshot-Erwartungen.
 */
public class ScreenshotExpectation {

    private final ThenExpectation base;

    public ScreenshotExpectation(ThenExpectation base) {
        this.base = base;
    }

    public String getSelector() {
        return (String) base.getParameterMap().getOrDefault("selector", "body");
    }

    public double getThreshold() {
        try {
            return Double.parseDouble(
                    String.valueOf(base.getParameterMap().getOrDefault("threshold", "0"))
            );
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public ThenExpectation getBase() {
        return base;
    }
}
