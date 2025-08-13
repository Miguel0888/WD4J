package de.bund.zrb.util;

import de.bund.zrb.type.script.WDEvaluateResult;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import java.util.ArrayList;
import java.util.List;

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

    public static boolean asBoolean(Object rv) {
        if (rv instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) rv).getValue();
        }
        throw new IllegalStateException("Expected BooleanValue, got: " + rv);
    }

    /**
     * Wandelt das Skript-Ergebnis in ein double[] um.
     * Akzeptiert:
     *  - WDEvaluateResult (SUCCESS mit Array-Result)
     *  - WDRemoteValue.ArrayRemoteValue
     *  - java.util.List (Numbers/Strings)
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
            if (s != null && s.startsWith("[") && s.endsWith("]")) {
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

        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }

        // Manche Remote-/Primitive-Werte haben sinnvolle toString()-Darstellung oder String-Payload
        if (v instanceof CharSequence) {
            try {
                return Double.parseDouble(v.toString().trim());
            } catch (NumberFormatException ignore) { /* unten weiter */ }
        }

        // Letzter Versuch: aus toString() pars(en) (z.B. "NumberValue{value=123.45}")
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
            vals.add(Double.parseDouble(p));
        }
        double[] out = new double[vals.size()];
        for (int i = 0; i < vals.size(); i++) out[i] = vals.get(i);
        return out;
    }
}
