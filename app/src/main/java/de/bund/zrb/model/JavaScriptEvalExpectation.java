package de.bund.zrb.model;

import de.bund.zrb.model.ThenExpectation;

import java.util.Map;

/**
 * Hilfsklasse zum typsicheren Zugriff auf JavaScript-Expectations.
 */
public class JavaScriptEvalExpectation {

    private final ThenExpectation base;

    public JavaScriptEvalExpectation(ThenExpectation base) {
        this.base = base;
    }

    public String getScript() {
        return (String) base.getParameterMap().getOrDefault("script", "return true;");
    }

    public ThenExpectation getBase() {
        return base;
    }
}
