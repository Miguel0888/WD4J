package de.bund.zrb.util;

public class WebDriverUtil {
    /**
     * Prüft, ob der übergebene Ausdruck eine Funktion ist. Wird benötigt, um zu entscheiden, ob ein `callFunction` oder
     * ein `evaluate`-Befehl ausgeführt werden soll.
     *
     * Erkennt folgende Konstrukte:
     * () => { ... }
     * x => x + 1
     * function() { return 42; }
     *
     * @param expr
     * @return
     */
    public static boolean isFunctionExpression(String expr) {
        if (expr == null) {
            return false;
        }

        String trimmed = expr.trim();

        // Arrow Function z. B. () => ..., x => ...
        boolean isArrow = trimmed.matches("^\\(?\\s*[^)]*\\)?\\s*=>.*");

        // Anonyme klassische Funktion: function(...) { ... }
        boolean isAnonymousFunction = trimmed.matches("^function\\s*\\(.*\\)\\s*\\{.*\\}$");

        return isArrow || isAnonymousFunction;
    }
}
