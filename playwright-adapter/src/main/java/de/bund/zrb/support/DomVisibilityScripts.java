package de.bund.zrb.support;

/** Provide robust DOM visibility checks as JS snippets. */
public final class DomVisibilityScripts {

    private DomVisibilityScripts() {
        // Prevent instantiation
    }

    /**
     * Check if element is truly visible:
     * - Not display:none, visibility not hidden/collapse
     * - Has client rects (rendered)
     * - No aria-hidden="true" or hidden attribute
     * - No PF "ui-helper-hidden" class
     * - All ancestor constraints must hold as well
     */
    public static final String IS_VISIBLE =
            "function() { " +
                    "  function visible(el) { " +
                    "    if (!el) return false; " +
                    "    if (el.nodeType !== 1) return false; " +
                    "    var style = window.getComputedStyle(el); " +
                    "    if (style.display === 'none') return false; " +
                    "    if (style.visibility === 'hidden' || style.visibility === 'collapse') return false; " +
                    "    if (el.hasAttribute('hidden')) return false; " +
                    "    if (el.getAttribute('aria-hidden') === 'true') return false; " +
                    "    if (el.classList && el.classList.contains('ui-helper-hidden')) return false; " +
                    "    if (el.getClientRects().length === 0) return false; " +
                    "    var p = el.parentElement; " +
                    "    while (p && p !== document.body) { " +
                    "      var ps = window.getComputedStyle(p); " +
                    "      if (ps.display === 'none') return false; " +
                    "      if (ps.visibility === 'hidden' || ps.visibility === 'collapse') return false; " +
                    "      if (p.hasAttribute('hidden')) return false; " +
                    "      if (p.getAttribute('aria-hidden') === 'true') return false; " +
                    "      if (p.classList && p.classList.contains('ui-helper-hidden')) return false; " +
                    "      p = p.parentElement; " +
                    "    } " +
                    "    return true; " +
                    "  } " +
                    "  return visible(this); " +
                    "}";

    /**
     * Invert IS_VISIBLE. Keep explicit function to avoid relying on negation of user-land JS truthiness.
     */
    public static final String IS_HIDDEN =
            "function() { " +
                    "  var f = " + IS_VISIBLE + "; " +
                    "  return !f.call(this); " +
                    "}";

    /** Wait for next paint twice to pass through CSS transitions reliably. */
    public static final String DOUBLE_RAF =
            "() => new Promise(res => requestAnimationFrame(() => requestAnimationFrame(res)))";
}
