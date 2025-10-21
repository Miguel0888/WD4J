package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.PlaywrightException;
import de.bund.zrb.type.script.*;
import de.bund.zrb.util.WebDriverUtil;

import java.util.*;

public class JSHandleImpl implements JSHandle {
    protected final WebDriver webDriver;
    protected final WDRemoteValue remoteValue;
    protected final WDTarget target;
    protected boolean disposed = false;

    public JSHandleImpl(WebDriver webDriver, WDRemoteValue remoteValue, WDTarget target) {
        this.webDriver = webDriver;
        this.remoteValue = remoteValue;
        this.target = target;
    }

    /**
     * Gibt das zugrunde liegende WDRemoteValue zurück (z.B. für interne Weiterverarbeitung).
     */
    public WDRemoteValue getRemoteValue() {
        return remoteValue;
    }

    /**
     * Ermittelt den WDHandle, falls dieses JSHandle ein referenziertes Remote-Objekt besitzt.
     * Gibt null zurück, wenn remoteValue kein Remote-Handle (z.B. nur ein primitiver Wert) ist.
     */
    private WDHandle getHandle() {
        // Viele WDRemoteValue-Implementierungen (z.B. NodeRemoteValue) besitzen eine getHandle()-Methode
        try {
            // Falls remoteValue ein Remote-Objekt repräsentiert, hat es einen Handle
            return (WDHandle) remoteValue.getClass().getMethod("getHandle").invoke(remoteValue);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ElementHandle asElement() {
        checkDisposed();
        // Prüfen, ob das zugrundeliegende JS-Objekt ein Element ist
        if ("node".equals(remoteValue.getType())) {
            // Optional: Bestätigen per instanceof Element im Browser
            Boolean isElement = Boolean.TRUE.equals(evaluate("(obj) => obj instanceof Element", null));
            if (isElement) {
                // Neues ElementHandleImpl erzeugen mit demselben WDRemoteValue
                return new ElementHandleImpl(webDriver, remoteValue, target);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        // Falls ein Remote-Handle existiert, beim Browser freigeben (disown)
        WDHandle handle = getHandle();
        if (handle != null) {
            webDriver.script().disown(Collections.singletonList(handle), target);
        }
        disposed = true;
    }

    /**
     * Führt einen JavaScript-Ausdruck im Kontext dieses Handles aus und gibt das Ergebnis zurück.
     * Diese Methode übergibt das referenzierte JS-Objekt als erstes Argument an den Ausdruck.
     * @param expression JavaScript-Expression oder -Funktion.
     * @param arg Optionales Argument, das als zusätzliches Argument an {@code expression} übergeben wird.
     * @return Der ausgewertete Wert (JSON-serialisierbarer Java-Wert) oder null (undefined), falls nicht serialisierbar.
     */
    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();
        WDEvaluateResult result;
        // Expression als Funktion?
        if (WebDriverUtil.isFunctionExpression(expression)) {
            // Argumente vorbereiten (erster Parameter ist das JS-Objekt dieses Handles)
            List<WDLocalValue> argsList = new ArrayList<>();
            // Das referenzierte Objekt als erstes Argument hinzufügen
            if (remoteValue != null) {
                argsList.add(WDLocalValue.fromObject(remoteValue));
            }
            // Optional: weiteres Nutzer-Argument hinzufügen
            if (arg != null) {
                argsList.add(WDLocalValue.fromObject(arg));
            }
            result = webDriver.script().callFunction(expression, true, target, argsList, null);
        } else {
            // Kein Funktionsausdruck – direkt auswerten (das JS-Objekt wird nicht benötigt)
            result = webDriver.script().evaluate(expression, target, true);
        }

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue resultValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            // Falls das Ergebnis nicht vollständig serialisierbar ist (enthält einen Remote-Handle), als undefined behandeln
            WDHandle handle = null;
            try {
                handle = (WDHandle) resultValue.getClass().getMethod("getHandle").invoke(resultValue);
            } catch (Exception ignore) { /* no handle method, or value is primitive/structured */ }
            if (handle != null) {
                // Referenz sofort freigeben, da der Nutzer kein JSHandle angefordert hat
                webDriver.script().disown(Collections.singletonList(handle), target);
                return null;  // non-serializable → undefined (null in Java)
            }
            // Serialisierbares Ergebnis in Java-Objekt konvertieren
            return convertRemoteValue(resultValue);
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            // JavaScript-Ausführung warf einen Fehler – Ausnahme weiterreichen
            WDExceptionDetails error = ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails();
            throw new PlaywrightException("Evaluation failed: " + error.getText());
        }
        return null;
    }

    /**
     * Führt einen JavaScript-Ausdruck im Kontext dieses Handles aus und gibt das Ergebnis als JSHandle zurück.
     * @param expression JavaScript-Expression oder -Funktion.
     * @param arg Optionales Argument für den Ausdruck.
     * @return JSHandle für den Auswertungsergebnis.
     */
    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        checkDisposed();
        WDEvaluateResult result;
        if (WebDriverUtil.isFunctionExpression(expression)) {
            List<WDLocalValue> argsList = new ArrayList<>();
            // Referenziertes Objekt als erstes Funktionsargument
            if (remoteValue != null) {
                argsList.add(WDLocalValue.fromObject(remoteValue));
            }
            if (arg != null) {
                argsList.add(WDLocalValue.fromObject(arg));
            }
            result = webDriver.script().callFunction(expression, true, target, argsList, null);
        } else {
            // Nicht-Funktionsausdruck: hier erzwingen wir Ergebnis-Handle (falls Objekt) durch Ownership-Einstellung
            result = webDriver.script().evaluate(expression, target, true /* default likely returns handle for objects */);
        }

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue resultValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            // Neues JSHandle für das Ergebnis erzeugen
            // Hinweis: resultValue kann einen Handle (für nicht serialisierbare Objekte) oder direkten Wert enthalten
            return new JSHandleImpl(webDriver, resultValue, target);
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            WDExceptionDetails error = ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails();
            throw new PlaywrightException("Evaluation failed: " + error.getText());
        }
        return null;
    }

    @Override
    public Map<String, JSHandle> getProperties() {
        checkDisposed();
        // Alle eigenen Properties-Schlüssel des Objekts ermitteln
        Object keysObj = evaluate("(obj) => Object.keys(obj)", null);
        if (!(keysObj instanceof List)) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) keysObj;
        Map<String, JSHandle> properties = new HashMap<>();
        for (String key : keys) {
            // Für jede Property einen JSHandle holen
            JSHandle propHandle = getProperty(key);
            properties.put(key, propHandle);
        }
        return properties;
    }

    @Override
    public JSHandle getProperty(String propertyName) {
        checkDisposed();
        // Einzelne Property via Funktion auslesen (liefert JSHandle)
        String script = "(obj, key) => obj[key]";
        return evaluateHandle(script, propertyName);
    }

    @Override
    public Object jsonValue() {
        checkDisposed();
        // JSON-Repräsentation des referenzierten Objekts zurückgeben
        // Wenn das Objekt nicht stringifizierbar ist (z.B. ein DOM-Element), geben wir ein leeres JSON-Objekt zurück.
        WDHandle handle = getHandle();
        if (handle != null) {
            // Nicht stringifizierbares Objekt → {}
            return new HashMap<String, Object>();
        }
        // Für serialisierbare Werte: vorhandenes WDRemoteValue in Java-Struktur umwandeln
        return convertRemoteValue(remoteValue);
    }

    @Override
    public String toString() {
        String info;
        // Freundliche Darstellung: handle-Wert oder Typ anzeigen
        WDHandle handle = getHandle();
        if (handle != null) {
            info = "handle=" + handle.value();
        } else {
            info = "value=" + convertRemoteValue(remoteValue);
        }
        return "JSHandle@" + target + "{" + info + "}";
    }

    // 🔹 Hilfsmethoden

    private void checkDisposed() {
        if (disposed) {
            throw new IllegalStateException("JSHandle has been disposed.");
        }
    }

    /**
     * Konvertiert ein WDRemoteValue rekursiv in entsprechende Java-Objekte (Primitiver Typ, Map, List etc.).
     * Nicht-serialisierbare Objekte (mit Remote-Handle) werden als leere JSON-Objekte dargestellt.
     */
    Object convertRemoteValue(WDRemoteValue rv) {
        if (rv == null) {
            return null;
        }
        String type = rv.getType();
        switch (type) {
            case "undefined":
            case "null":
                return null;
            case "boolean":
                // WDPrimitiveProtocolValue.BooleanValue hat getValue()
                return ((WDPrimitiveProtocolValue.BooleanValue) rv).getValue();
            case "number":
                return ((WDPrimitiveProtocolValue.NumberValue) rv).getValue();
            case "string":
                return ((WDPrimitiveProtocolValue.StringValue) rv).getValue();
            case "bigint":
                // BigInt als BigInteger zurückgeben
                String bigIntStr = ((WDPrimitiveProtocolValue.BigIntValue) rv).getValue();
                return new java.math.BigInteger(bigIntStr);
            case "array":
                // ArrayRemoteValue: jedes Element umwandeln
                List<WDRemoteValue> listVal = ((WDRemoteValue.ArrayRemoteValue) rv).getValue();
                List<Object> resultList = new ArrayList<>(listVal.size());
                for (WDRemoteValue item : listVal) {
                    resultList.add(convertRemoteValue(item));
                }
                return resultList;
            case "object":
                // ObjectRemoteValue mit Key-Value-Einträgen
                Map<WDRemoteValue, WDRemoteValue> objMap = ((WDRemoteValue.ObjectRemoteValue) rv).getValue();
                Map<String, Object> resultMap = new HashMap<>();
                for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : objMap.entrySet()) {
                    // Schlüssel sind Strings (WDPrimitiveProtocolValue.StringValue)
                    String key = (String) convertRemoteValue(entry.getKey());
                    Object value = convertRemoteValue(entry.getValue());
                    resultMap.put(key, value);
                }
                return resultMap;
            case "date":
                // DateRemoteValue: als ISO-String zurückgeben
                String dateStr = ((WDRemoteValue.DateRemoteValue) rv).getValue();
                return dateStr;
            case "regexp":
                // RegExpRemoteValue: als String /pattern/flags repräsentieren
                String regex = ((WDRemoteValue.RegExpRemoteValue) rv).getValue().toString();
                return regex;
            default:
                // Andere nicht direkt unterstützte Typen (function, node, map, set, etc.)
                // Prüfen, ob ein Remote-Handle existiert → falls ja, nicht stringifizierbar → {}
                try {
                    WDHandle rvHandle = (WDHandle) rv.getClass().getMethod("getHandle").invoke(rv);
                    if (rvHandle != null) {
                        return new HashMap<String, Object>();  // nicht serialisierbar → leeres Objekt
                    }
                } catch (Exception ignore) {}
                // Falls kein Handle: entweder ein strukturierter Sondertyp (z.B. MapRemoteValue mit Einträgen)
                // oder einfach nicht erkannt – als leeres Objekt zurückgeben
                return new HashMap<String, Object>();
        }
    }
}
