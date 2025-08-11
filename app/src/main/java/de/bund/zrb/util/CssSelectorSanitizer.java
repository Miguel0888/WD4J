package de.bund.zrb.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitize CSS selectors for JSF/PrimeFaces clientIds that contain colons.
 * Replace occurrences of "#id:with:colon" by "[id='id:with:colon']"
 * without touching already valid attribute selectors or classes.
 */
public final class CssSelectorSanitizer {

    // Match occurrences like "#foo:bar:baz" but not "#foo" (no colon).
    private static final Pattern HASH_ID_WITH_COLON = Pattern.compile("#([^\\s>#.:\\[]*:[^\\s>#.:\\[]*)");

    private CssSelectorSanitizer() { }

    /** Make a selector safe for querySelectorAll if it contains JSF-style ids with colon. */
    public static String sanitize(String css) {
        if (css == null || css.trim().isEmpty()) return css;

        String result = css;

        // Replace "#foo:bar" with "[id='foo:bar']"
        Matcher m = HASH_ID_WITH_COLON.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String id = m.group(1);
            m.appendReplacement(sb, "[id='" + escapeForAttr(id) + "']");
        }
        m.appendTail(sb);
        result = sb.toString();

        // If selector is a naked id like "#foo:bar" only, it got converted above.
        // For pure id type we could also prefer [id='...'] always.

        return result;
    }

    private static String escapeForAttr(String s) {
        // Minimal escaping for attribute literal: escape single quotes
        return s.replace("'", "\\'");
    }
}
