package de.bund.zrb.model;

import java.util.Arrays;
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

        screenshot.addField("selector", "CSS Selector", "body", String.class);
        screenshot.addField("threshold", "Fehlertoleranz (%)", 0.01, Double.class);
        screenshot.addField("fullPage", "Vollständige Seite", false, Boolean.class);
        screenshot.addField("maskColor", "Maskenfarbe", "#FF00FF", String.class);
        screenshot.addField("omitBackground", "Hintergrund ausblenden", false, Boolean.class);
        screenshot.addField("quality", "Qualität (0–100)", 100, Integer.class);
        screenshot.addField("scale", "Skalierung", "device", String.class, Arrays.asList("device", "css"));
        screenshot.addField("style", "CSS Stylesheet", "", String.class);
        screenshot.addField("timeout", "Timeout (ms)", 30000, Integer.class);
        screenshot.addField("type", "Dateiformat", "png", String.class, Arrays.asList("png", "jpeg", "webp"));

        screenshot.addField("clipX", "Clip X", null, Integer.class);
        screenshot.addField("clipY", "Clip Y", null, Integer.class);
        screenshot.addField("clipWidth", "Clip Breite", null, Integer.class);
        screenshot.addField("clipHeight", "Clip Höhe", null, Integer.class);

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

        js.addField("script", "JavaScript Ausdruck", "return document.title", String.class);

        register(js);
    }


}
