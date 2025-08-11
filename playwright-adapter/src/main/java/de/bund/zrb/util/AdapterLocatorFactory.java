package de.bund.zrb.util;

import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;

/**
 * Build WDLocator from explicit LocatorType + selector token.
 * Falls type == null, fall back to legacy string heuristics for backwards compatibility.
 */
public final class AdapterLocatorFactory {
    private AdapterLocatorFactory() {
        // Prevent instantiation
    }

    public static WDLocator<?> create(LocatorType type, String selector) {
        String s = selector != null ? selector.trim() : "";
        if (type == null) {
            return createFromHeuristic(s);
        }

        switch (type) {
            case XPATH:
                // Pass raw XPath (do not prefix with "xpath=")
                return new WDLocator.XPathLocator(stripEnginePrefix(s));

            case CSS:
                // Pass raw CSS
                return new WDLocator.CssLocator(stripEnginePrefix(s));

            case ID:
                // Always use attribute selector to support JSF ":" in ids
                if (s.startsWith("#") || s.startsWith("[id=")) {
                    return new WDLocator.CssLocator(s);
                }
                return new WDLocator.CssLocator("[id='" + escapeSingleQuotes(s) + "']");

            case TEXT:
                // Avoid BiDi innerText: map to XPath by text
                return new WDLocator.XPathLocator(buildTextContainsXPath(s));

            case LABEL:
                return new WDLocator.XPathLocator(buildLabelXPath(s));

            case PLACEHOLDER:
                return new WDLocator.XPathLocator(
                        "//*[@placeholder and contains(normalize-space(@placeholder)," + xpathLiteral(s) + ")]"
                );

            case ALTTEXT:
                return new WDLocator.XPathLocator(
                        "//*[@alt and contains(normalize-space(@alt)," + xpathLiteral(s) + ")]"
                );

            case ROLE:
                // Expect token like "role=button;name=Senden" or just "button"
                String role = null;
                String name = null;
                if (s.indexOf('=') < 0 && s.indexOf('[') < 0) {
                    role = s;
                } else {
                    String[] parts = s.split(";");
                    for (int i = 0; i < parts.length; i++) {
                        String p = parts[i].trim();
                        int idx = p.indexOf('=');
                        if (idx > 0) {
                            String k = p.substring(0, idx).trim();
                            String v = p.substring(idx + 1).trim();
                            if ("role".equalsIgnoreCase(k)) role = v;
                            if ("name".equalsIgnoreCase(k)) name = v;
                        }
                    }
                }
                WDLocator.AccessibilityLocator.Value value = new WDLocator.AccessibilityLocator.Value(name, role);
                return new WDLocator.AccessibilityLocator(value);

            default:
                return createFromHeuristic(s);
        }
    }

    /** Backward-compatible creation when no explicit type is provided. */
    public static WDLocator<?> create(String selector) {
        return create(null, selector);
    }

    // -------- Heuristic fallback (legacy behavior) --------

    private static WDLocator<?> createFromHeuristic(String selector) {
        String s = selector != null ? selector.trim() : "";
        if (s.startsWith("/") || s.startsWith("(")) {
            return new WDLocator.XPathLocator(s);
        }
        if (s.startsWith("text=")) {
            // Legacy innerText prefix: prefer XPath mapping
            String txt = s.substring(5);
            return new WDLocator.XPathLocator(buildTextContainsXPath(txt));
        }
        if (s.startsWith("aria=")) {
            String rest = s.substring(5).trim();
            String role = null;
            String name = null;

            int nameStart = rest.indexOf("[name=");
            if (nameStart >= 0) {
                role = rest.substring(0, nameStart).trim();
                String namePart = rest.substring(nameStart);
                name = parseNameFromBrackets(namePart);
            } else if (rest.startsWith("[")) {
                name = parseNameFromBrackets(rest);
            } else {
                role = rest.isEmpty() ? null : rest;
            }
            WDLocator.AccessibilityLocator.Value value = new WDLocator.AccessibilityLocator.Value(name, role);
            return new WDLocator.AccessibilityLocator(value);
        }
        return new WDLocator.CssLocator(s);
    }

    private static String parseNameFromBrackets(String bracketPart) {
        // Expect: [name="..."]
        int eq = bracketPart.indexOf('=');
        int quote1 = bracketPart.indexOf('"', eq);
        int quote2 = bracketPart.indexOf('"', quote1 + 1);
        if (eq >= 0 && quote1 >= 0 && quote2 >= 0) {
            return bracketPart.substring(quote1 + 1, quote2);
        }
        return null;
    }

    // -------- XPath builders --------

    /** Build XPath that matches any element containing visible text (normalized). */
    private static String buildTextContainsXPath(String text) {
        return "//*[contains(normalize-space(string(.))," + xpathLiteral(text) + ")]";
    }

    /** Build XPath for label associations. */
    private static String buildLabelXPath(String label) {
        String lit = xpathLiteral(label);
        return "//*[@id=//label[normalize-space(string(.))=" + lit + "]/@for]"
                + " | //label[normalize-space(string(.))=" + lit + "]//input"
                + " | //label[normalize-space(string(.))=" + lit + "]//textarea"
                + " | //label[normalize-space(string(.))=" + lit + "]//select";
    }

    /** Quote arbitrary string as XPath literal. */
    private static String xpathLiteral(String s) {
        if (s.indexOf('\'') == -1) return "'" + s + "'";
        if (s.indexOf('"') == -1) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        char[] arr = s.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            char c = arr[i];
            if (c == '\'') sb.append("\"").append(c).append("\"");
            else sb.append("'").append(c).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String stripEnginePrefix(String s) {
        if (s == null) return null;
        if (s.startsWith("xpath=")) return s.substring(6).trim();
        if (s.startsWith("css=")) return s.substring(4).trim();
        return s;
    }

    private static String escapeSingleQuotes(String s) {
        return s == null ? null : s.replace("'", "\\'");
    }

    public static LocatorType inferType(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.regionMatches(true, 0, "xpath=", 0, 6)) return LocatorType.XPATH;
        if (s.regionMatches(true, 0, "css=", 0, 4))   return LocatorType.CSS;
        if (s.regionMatches(true, 0, "text=", 0, 5))  return LocatorType.TEXT;
        if (s.regionMatches(true, 0, "aria=", 0, 5) ||
                s.regionMatches(true, 0, "role=", 0, 5))  return LocatorType.ROLE;
        if (s.regionMatches(true, 0, "label=", 0, 6)) return LocatorType.LABEL;
        if (s.startsWith("/") || s.startsWith("("))   return LocatorType.XPATH; // nur hier (API-Zwang)
        return LocatorType.CSS; // Default
    }

    public static String stripKnownPrefix(String raw) {
        String s = raw == null ? "" : raw.trim();
        String[] p = {"xpath=","css=","text=","aria=","role=","label=","placeholder=","title=","alt=","altText=","data-testid="};
        for (String pref : p) {
            if (s.regionMatches(true, 0, pref, 0, pref.length())) {
                return s.substring(pref.length());
            }
        }
        return s;
    }
}
