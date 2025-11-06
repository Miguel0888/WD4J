package de.bund.zrb.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Build a anchored regex from plain cell text; numbers become capture groups. */
public final class RegexFromTextBuilder {
    private RegexFromTextBuilder() { }

    /** Build anchored regex with flexible whitespace. */
    public static String buildAnchoredRegex(String cellText) {
        return build(cellText, true, true);
    }

    /** Build regex; configure anchors and flexible whitespace handling. */
    public static String build(String cellText, boolean anchor, boolean flexibleWs) {
        if (cellText == null) cellText = "";

        StringBuilder out = new StringBuilder();
        if (anchor) out.append("(?s)^"); // DOTALL + anchor start

        char[] a = cellText.toCharArray();
        int i = 0;
        while (i < a.length) {
            char ch = a[i];

            if (Character.isDigit(ch)) {
                // Consume a "number block": digits possibly interleaved with whitespace
                int j = i + 1;
                while (j < a.length) {
                    char cj = a[j];
                    if (Character.isDigit(cj)) { j++; continue; }
                    if (Character.isWhitespace(cj)) { j++; continue; }
                    break;
                }
                appendNumberGroup(out, flexibleWs);
                i = j;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                // Compress any whitespace run into \s+ or literal space based on flag
                int j = i + 1;
                while (j < a.length && Character.isWhitespace(a[j])) j++;
                if (flexibleWs) out.append("\\s+"); else out.append(" ");
                i = j;
                continue;
            }

            // Consume literal (non-digit, non-whitespace) run and escape
            int j = i + 1;
            while (j < a.length && !Character.isDigit(a[j]) && !Character.isWhitespace(a[j])) j++;
            String literal = new String(a, i, j - i);
            out.append(Pattern.quote(literal));
            i = j;
        }

        if (anchor) out.append("$");
        return out.toString();
    }

    /** Append one capturing group for a number with optional inner whitespace. */
    private static void appendNumberGroup(StringBuilder out, boolean flexibleWs) {
        if (flexibleWs) {
            // Capture digits possibly separated by whitespace: "1 234  56"
            out.append("((?:\\d\\s*)+)");
        } else {
            out.append("(\\d+)");
        }
    }

    /** Utility: strip all whitespace from a captured group value. */
    public static String normalizeGroup(String group) {
        return group == null ? "" : group.replaceAll("\\s+", "");
    }
}
