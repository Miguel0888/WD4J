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
        screenshot.addField("fullPage", "Vollständige Seite", "false");
        screenshot.addField("maskColor", "Maskenfarbe", "#FF00FF");
        screenshot.addField("omitBackground", "Hintergrund ausblenden", "false");
        screenshot.addField("quality", "Qualität (0–100)", "");
        screenshot.addField("scale", "Skalierung", "device");
        screenshot.addField("style", "CSS Stylesheet", "");
        screenshot.addField("timeout", "Timeout (ms)", "30000");
        screenshot.addField("type", "Dateiformat", "png");

        screenshot.addField("clipX", "Clip X", "");
        screenshot.addField("clipY", "Clip Y", "");
        screenshot.addField("clipWidth", "Clip Breite", "");
        screenshot.addField("clipHeight", "Clip Höhe", "");

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
