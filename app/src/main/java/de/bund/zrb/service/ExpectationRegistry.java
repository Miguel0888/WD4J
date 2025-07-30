package de.bund.zrb.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton-Registry für alle verfügbaren Erwartungstypen.
 */
public class ExpectationRegistry {

    private static final ExpectationRegistry INSTANCE = new ExpectationRegistry();
    private final Map<String, ExpectationTypeDefinition> definitions = new HashMap<>();

    private ExpectationRegistry() {
        registerDefaults();
    }

    public static ExpectationRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ExpectationTypeDefinition definition) {
        definitions.put(definition.getType(), definition);
    }

    public ExpectationTypeDefinition get(String type) {
        return definitions.get(type);
    }

    public Collection<ExpectationTypeDefinition> getAll() {
        return definitions.values();
    }

    private void registerDefaults() {
        // Screenshot-Erwartung
        ExpectationTypeDefinition screenshot = new ExpectationTypeDefinition(
                "screenshot",
                "Screenshot vergleichen",
                params -> {
                    if (!params.containsKey("selector")) {
                        throw new ValidationException("Selector darf nicht leer sein.");
                    }
                });
        screenshot.addField("selector", "CSS Selector", "body");
        screenshot.addField("threshold", "Fehlertoleranz (%)", "0");

        register(screenshot);

        // JavaScript-Erwartung
        ExpectationTypeDefinition js = new ExpectationTypeDefinition(
                "js-eval",
                "JavaScript Erwartung (Rückgabewert)",
                params -> {
                    String code = (String) params.get("script");
                    if (code == null || code.trim().isEmpty()) {
                        throw new ValidationException("JavaScript darf nicht leer sein.");
                    }
                });
        js.addField("script", "JavaScript Ausdruck", "return document.title");

        register(js);
    }
}
