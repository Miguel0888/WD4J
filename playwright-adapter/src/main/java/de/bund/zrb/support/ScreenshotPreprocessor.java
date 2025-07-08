package de.bund.zrb.support;

import com.microsoft.playwright.Locator;
import de.bund.zrb.type.script.WDEvaluateResult;

import java.util.List;

/**
 * Utility-Klasse für Playwright-ähnliche Screenshot-Preprocessing-Features
 * wie Animationen stoppen, Caret verstecken oder Masken setzen.
 * Alle Methoden sind stateless und können mehrfach verwendet werden.
 */
public final class ScreenshotPreprocessor {

    private ScreenshotPreprocessor() {
        // Prevent instantiation
    }

    /**
     * Fügt einen globalen CSS-Block ein, der alle CSS-Animationen & Transitionen deaktiviert.
     *
     * @param evaluateFunction Funktion, um JavaScript auszuführen
     */
    public static void disableAnimations(Evaluator evaluateFunction) {
        String script =
                "(() => {" +
                        "  const style = document.createElement('style');" +
                        "  style.id = '__playwright_no_animations__';" +
                        "  style.innerHTML = '* {" +
                        "    animation: none !important;" +
                        "    transition: none !important;" +
                        "  }';" +
                        "  document.head.appendChild(style);" +
                        "})();";
        evaluateFunction.evaluate(script);
    }

    /**
     * Fügt einen globalen CSS-Block ein, der den Text-Caret unsichtbar macht.
     *
     * @param evaluateFunction Funktion, um JavaScript auszuführen
     */
    public static void hideCaret(Evaluator evaluateFunction) {
        String script =
                "(() => {" +
                        "  const style = document.createElement('style');" +
                        "  style.id = '__playwright_hide_caret__';" +
                        "  style.innerHTML = '* {" +
                        "    caret-color: transparent !important;" +
                        "  }';" +
                        "  document.head.appendChild(style);" +
                        "})();";
        evaluateFunction.evaluate(script);
    }

    /**
     * Maskiert alle angegebenen Locators mit einer farbigen Overlay-Box.
     *
     * @param locators Liste der Locators
     * @param maskColor CSS-Farbwert (z. B. #FF00FF)
     * @param evaluateFunction Funktion, um JavaScript auszuführen
     */
    public static void applyMask(List<Locator> locators, String maskColor, Evaluator evaluateFunction) {
        if (locators == null || locators.isEmpty()) {
            return;
        }
        String safeMaskColor = maskColor != null ? maskColor : "#FF00FF";
        for (Locator locator : locators) {
            String script =
                    "(() => {" +
                            "  const el = document.querySelector('" + locator.toString() + "');" +
                            "  if (!el) return;" +
                            "  const rect = el.getBoundingClientRect();" +
                            "  const overlay = document.createElement('div');" +
                            "  overlay.style.position = 'absolute';" +
                            "  overlay.style.left = rect.left + window.scrollX + 'px';" +
                            "  overlay.style.top = rect.top + window.scrollY + 'px';" +
                            "  overlay.style.width = rect.width + 'px';" +
                            "  overlay.style.height = rect.height + 'px';" +
                            "  overlay.style.backgroundColor = '" + safeMaskColor + "';" +
                            "  overlay.style.zIndex = 2147483647;" +
                            "  overlay.className = '__playwright_mask__';" +
                            "  document.body.appendChild(overlay);" +
                            "})();";
            evaluateFunction.evaluate(script);
        }
    }

    /**
     * Entfernt alle temporären Preprocessing-Styles & Masken.
     *
     * @param evaluateFunction Funktion, um JavaScript auszuführen
     */
    public static void restore(Evaluator evaluateFunction) {
        String script =
                "(() => {" +
                        "  const s1 = document.getElementById('__playwright_no_animations__');" +
                        "  if (s1) s1.remove();" +
                        "  const s2 = document.getElementById('__playwright_hide_caret__');" +
                        "  if (s2) s2.remove();" +
                        "  document.querySelectorAll('.__playwright_mask__').forEach(e => e.remove());" +
                        "})();";
        evaluateFunction.evaluate(script);
    }

    /**
     * Functional Interface für Evaluate-Calls.
     */
    @FunctionalInterface
    public interface Evaluator {
        void evaluate(String script);
    }
}
