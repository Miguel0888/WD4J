package de.bund.zrb.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hält zur Laufzeit alle aktuell verfügbaren Variablenwerte.
 * - Key: Variablenname (z.B. "username")
 * - Value: konkreter Stringwert (z.B. "bob42")
 *
 * Wichtig:
 * - Case darf Suite überschreiben darf Root überschreiben.
 * - D.h. spätere put() überschreibt ältere Werte.
 */
public class ScenarioState {

    private final Map<String,String> values = new LinkedHashMap<>();

    public void put(String name, String concreteValue) {
        if (name == null) return;
        values.put(name, concreteValue);
    }

    public String get(String name) {
        if (name == null) return null;
        return values.get(name);
    }

    public Map<String,String> snapshot() {
        return new LinkedHashMap<>(values);
    }
}
