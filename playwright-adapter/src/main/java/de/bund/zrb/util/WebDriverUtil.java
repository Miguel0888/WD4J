package de.bund.zrb.util;

import de.bund.zrb.type.script.WDEvaluateResult;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Hilfsfunktionen zum robusten Entpacken und Konvertieren von BiDi-Ergebnissen.
 */
public final class WebDriverUtil {
    private WebDriverUtil() {}

    /**
     * Prüft, ob der übergebene Ausdruck eine Funktion ist. Wird benötigt, um zu entscheiden, ob ein
     * {@code callFunction} oder ein {@code evaluate}-Befehl ausgeführt werden soll.
     *
     * Erkennt u. a.:
     *  - () => { ... }
     *  - x => x + 1
     *  - function() { return 42; }
     */
    public static boolean isFunctionExpression(String expr) {
        if (expr == null) return false;
        String trimmed = expr.trim();
        boolean isArrow = trimmed.matches("^\\(?\\s*[^)]*\\)?\\s*=>.*");
        boolean isAnonymousFunction = trimmed.matches("^function\\s*\\(.*\\)\\s*\\{.*\\}$");
        return isArrow || isAnonymousFunction;
    }

    // --------------------------------------------------------------------------------------------
    // BOOLEAN
    // --------------------------------------------------------------------------------------------

    /** Entpackt ein WDEvaluateResult (Success/Error) zu boolean. */
    public static boolean asBoolean(WDEvaluateResult res) {
        if (res instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException(
                    "Evaluation error: " + ((WDEvaluateResult.WDEvaluateResultError) res).getExceptionDetails());
        }
        if (res instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) res).getResult();
            return asBoolean(v);
        }
        throw new IllegalStateException("Unexpected result type: " + (res == null ? "null" : res.getClass().getName()));
    }

    /** Interpretiert einen WDRemoteValue als boolean. */
    public static boolean asBoolean(WDRemoteValue v) {
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) v).getValue();
        }
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
            String raw = ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
            try { return Double.parseDouble(raw) != 0.0; } catch (NumberFormatException e) { return false; }
        }
        if (v instanceof WDPrimitiveProtocolValue.StringValue) {
            String s = ((WDPrimitiveProtocolValue.StringValue) v).getValue();
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        // Andere Typen (Array/Object/Null) → nicht truthy
        return false;
    }

    /**
     * Fallback-Variante: akzeptiert Java-Primitives sowie BiDi-Typen.
     * Bequem für Call-Sites, die noch nicht sauber typisiert sind.
     */
    public static boolean asBoolean(Object rv) {
        if (rv == null) return false;

        if (rv instanceof Boolean) return (Boolean) rv;
        if (rv instanceof Number)  return ((Number) rv).doubleValue() != 0.0;
        if (rv instanceof CharSequence) {
            String s = rv.toString();
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        if (rv instanceof WDEvaluateResult) return asBoolean((WDEvaluateResult) rv);
        if (rv instanceof WDRemoteValue)    return asBoolean((WDRemoteValue) rv);

        throw new IllegalStateException("Cannot convert to boolean: " + rv.getClass().getName());
    }

    // --------------------------------------------------------------------------------------------
    // DOUBLE[]
    // --------------------------------------------------------------------------------------------

    /**
     * Wandelt das Skript-Ergebnis in ein double[] um.
     * Akzeptiert:
     *  - WDEvaluateResult (SUCCESS mit Array-Result)
     *  - WDRemoteValue.ArrayRemoteValue
     *  - java.util.List (Numbers/Strings/WDPrimitiveProtocolValues)
     *  - double[] / Number[] / Object[]
     *  - String im Format "[x,y,w,h]" (Fallback)
     */
    public static double[] asDoubleArray(Object res) {
        Object payload = res;

        // 1) WDEvaluateResult entpacken
        if (payload instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new IllegalStateException("Script error: "
                    + ((WDEvaluateResult.WDEvaluateResultError) payload).getExceptionDetails());
        }
        if (payload instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            payload = ((WDEvaluateResult.WDEvaluateResultSuccess) payload).getResult();
        }

        // 2) WDRemoteValue-Array?
        if (payload instanceof WDRemoteValue.ArrayRemoteValue) {
            List<WDRemoteValue> arr = ((WDRemoteValue.ArrayRemoteValue) payload).getValue();
            return listToDoubleArray(arr);
        }

        // 3) Plain Java List?
        if (payload instanceof List<?>) {
            return listToDoubleArray((List<?>) payload);
        }

        // 4) Primitive Arrays?
        if (payload instanceof double[]) {
            return (double[]) payload;
        }
        if (payload instanceof Number[]) {
            Number[] ns = (Number[]) payload;
            double[] out = new double[ns.length];
            for (int i = 0; i < ns.length; i++) out[i] = ns[i] == null ? Double.NaN : ns[i].doubleValue();
            return out;
        }
        if (payload instanceof Object[]) {
            Object[] os = (Object[]) payload;
            double[] out = new double[os.length];
            for (int i = 0; i < os.length; i++) out[i] = toDouble(os[i]);
            return out;
        }

        // 5) String-Fallback (z.B. "[10.5, 20, 300, 200]")
        if (payload instanceof String) {
            return parseDoublesFromString((String) payload);
        }

        // 6) Letzter Fallback: toString() versuchen
        if (payload != null) {
            String s = payload.toString();
            if (s.startsWith("[") && s.endsWith("]")) {
                return parseDoublesFromString(s);
            }
        }

        throw new IllegalArgumentException("Unsupported result for asDoubleArray: "
                + (payload == null ? "null" : payload.getClass().getName()));
    }

    private static double[] listToDoubleArray(List<?> list) {
        if (list == null) return new double[0];
        double[] out = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = toDouble(list.get(i));
        }
        return out;
    }

    private static double toDouble(Object v) {
        if (v == null) return Double.NaN;

        // Direkte Java-Typen
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof CharSequence) {
            try { return Double.parseDouble(v.toString().trim()); }
            catch (NumberFormatException ignore) { /* unten weiter */ }
        }

        // BiDi-Primitive explizit behandeln
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
            String raw = ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
            return parseNumber(raw);
        }
        if (v instanceof WDPrimitiveProtocolValue.StringValue) {
            String raw = ((WDPrimitiveProtocolValue.StringValue) v).getValue();
            return parseNumber(raw);
        }
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) v).getValue() ? 1.0 : 0.0;
        }

        // Letzter Versuch: aus toString() extrahieren (z.B. "NumberValue{value=123.45}")
        String s = v.toString();
        if (s != null) {
            String digits = s.replaceAll("[^0-9eE+\\-.]", "");
            if (!digits.isEmpty()) {
                try { return Double.parseDouble(digits); } catch (NumberFormatException ignore) {}
            }
        }

        throw new IllegalArgumentException("Cannot convert to double: " + v.getClass().getName());
    }

    private static double[] parseDoublesFromString(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) return new double[0];

        String[] parts = trimmed.split("[,;\\s]+");
        List<Double> vals = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p.isEmpty()) continue;
            vals.add(parseNumber(p));
        }
        double[] out = new double[vals.size()];
        for (int i = 0; i < vals.size(); i++) out[i] = vals.get(i);
        return out;
    }

    private static double parseNumber(String raw) {
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid number format: \"" + raw + "\"", e);
        }
    }

    public static Object unwrap(WDEvaluateResult r) {
        if (r instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Evaluation error: "
                    + ((WDEvaluateResult.WDEvaluateResultError) r).getExceptionDetails());
        }
        if (!(r instanceof WDEvaluateResult.WDEvaluateResultSuccess)) {
            return null;
        }
        WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
        return unwrapRemoteValue(v);
    }

    private static Object unwrapRemoteValue(WDRemoteValue v) {
        if (v == null) return null;

        // Primitives
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) v).getValue();
        }
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
            String raw = ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
            try { return Double.parseDouble(raw); } catch (NumberFormatException e) { return raw; }
        }
        if (v instanceof WDPrimitiveProtocolValue.StringValue) {
            return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
        }

        // Arrays -> List<Object>
        if (v instanceof WDRemoteValue.ArrayRemoteValue) {
            List<WDRemoteValue> arr = ((WDRemoteValue.ArrayRemoteValue) v).getValue();
            List<Object> out = new ArrayList<>(arr.size());
            for (WDRemoteValue it : arr) out.add(unwrapRemoteValue(it));
            return out;
        }

        // Nodes (Caller kann daraus einen Handle bauen, falls benötigt)
        if (v instanceof WDRemoteValue.NodeRemoteValue) {
            return v;
        }

        // Fallback: ungekanntes RemoteValue roh zurückgeben
        return v;
    }

}
