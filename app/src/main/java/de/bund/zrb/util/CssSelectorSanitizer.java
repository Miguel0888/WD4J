package de.bund.zrb.util;

public final class CssSelectorSanitizer {

    private CssSelectorSanitizer() {}

    /** Haupt-Einstieg */
    public static String sanitize(String selector) {
        if (selector == null) return null;
        String s = selector.trim();

        // 1) #id-Tokens in [id='…'] umschreiben, wenn sie JSF-Doppelpunkte enthalten
        s = normalizeHashIds(s);

        // 2) Nach [id='…'] direkt folgende :id_* -Tokens in den id-Wert mergen
        s = mergeDanglingJsfIdSuffix(s);

        System.out.println("[Sanitized] " + s);
        return s;
    }

    /** Schritt 1: #foo:bar -> [id='foo:bar'] (Token-basiert, nicht regex) */
    private static String normalizeHashIds(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0, n = s.length();

        while (i < n) {
            char c = s.charAt(i);

            if (c == '#') {
                int start = i++;                 // hinter '#'
                StringBuilder ident = new StringBuilder();
                boolean hasColon = false;

                while (i < n) {
                    char ch = s.charAt(i);
                    if (ch == '\\') {             // Escape übernehmen
                        if (i + 1 < n) {
                            ident.append(ch).append(s.charAt(i + 1));
                            i += 2;
                        } else {
                            ident.append(ch);
                            i++;
                        }
                    } else if (isIdentChar(ch)) { // Teil des Identifiers
                        if (ch == ':') hasColon = true;
                        ident.append(ch);
                        i++;
                    } else {
                        break;                      // Identifier zu Ende
                    }
                }

                if (ident.length() == 0) {
                    // nur '#', kein Name -> pass through
                    out.append('#');
                    continue;
                }

                String id = ident.toString();
                if (hasColon) {
                    // #a:b:c -> [id='a:b:c'] (Backslashes im Wert erhalten)
                    out.append("[id='").append(unescapeForAttr(id)).append("']");
                } else {
                    // normales #id bleibt unverändert
                    out.append('#').append(id);
                }
                // keine i++ mehr, weil wir schon weitergelaufen sind
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Schritt 2: [id='a:b'] :id_c  -> [id='a:b:id_c'] */
    private static String mergeDanglingJsfIdSuffix(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0, n = s.length();

        while (i < n) {
            int idStart = s.indexOf("[id=", i);
            if (idStart < 0) {
                out.append(s, i, n);
                break;
            }

            // alles bis zum id-Attribut übernehmen
            out.append(s, i, idStart);
            int bracketOpen = idStart;

            // finde öffnende Klammer '[', dann '=' und den gequoteten Wert
            int eq = s.indexOf('=', bracketOpen + 1);
            if (eq < 0) { // defensiv
                out.append(s.substring(idStart));
                break;
            }

            int quoteStart = skipSpaces(s, eq + 1);
            if (quoteStart >= n) { out.append(s.substring(idStart)); break; }
            char quote = s.charAt(quoteStart);
            if (quote != '\'' && quote != '"') { // unquoted id-Wert -> uns nicht anpacken
                out.append(s.substring(idStart));
                break;
            }

            int valStart = quoteStart + 1;
            int valEnd = findMatchingQuote(s, valStart, quote);
            if (valEnd < 0) { out.append(s.substring(idStart)); break; }

            String idVal = s.substring(valStart, valEnd);

            int closingBracket = s.indexOf(']', valEnd + 1);
            if (closingBracket < 0) { out.append(s.substring(idStart)); break; }

            // id-Attribut bis inkl. ']' übernehmen (vorerst)
            out.append(s, idStart, closingBracket + 1);

            // nach dem id-Attribut schauen, ob direkt :id_* folgt
            int k = closingBracket + 1;
            k = skipSpaces(s, k);

            if (k < n && s.charAt(k) == ':') {
                int pseudoStart = k + 1;
                int pseudoEnd = pseudoStart;

                while (pseudoEnd < n && isIdentChar(s.charAt(pseudoEnd))) {
                    pseudoEnd++;
                }
                String pseudo = s.substring(pseudoStart, pseudoEnd);

                // Nur mergen, wenn es wie unser JSF/PrimeFaces Suffix aussieht
                if (pseudo.startsWith("id_")) {
                    // idVal : pseudo -> idVal + ":" + pseudo
                    String merged = idVal + ":" + pseudo;
                    // überschreibe das bisher ausgegebene [id='…'] mit neuem Wert
                    out.setLength(out.length() - (closingBracket + 1 - idStart));
                    out.append("[id='").append(merged).append("']");

                    // den :id_* Token überspringen
                    i = pseudoEnd;
                    continue;
                }
            }

            // Kein Merge – normal weiter
            i = closingBracket + 1;
        }

        return out.toString();
    }

    private static int skipSpaces(String s, int i) {
        int n = s.length();
        while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int findMatchingQuote(String s, int from, char quote) {
        int n = s.length();
        int i = from;
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                i += 2; // escaped char
            } else if (ch == quote) {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static boolean isIdentChar(char ch) {
        // stark vereinfacht: reicht für Namen wie id_dropdown_4, nth-of-type, usw.
        return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':' || ch == '\\';
    }

    private static String unescapeForAttr(String id) {
        // Wir lassen Backslashes wie sie sind, weil der Browser sie im Attributwert
        // nicht als Escape interpretiert. Nur einfache Quotes sichern wir ab.
        return id.replace("'", "\\'");
    }
}
