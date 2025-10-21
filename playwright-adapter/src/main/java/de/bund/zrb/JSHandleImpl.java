package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.PlaywrightException;
import de.bund.zrb.type.script.*;
import de.bund.zrb.util.ScriptUtils;
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

    /** Expose underlying RemoteValue for internal use. */
    public WDRemoteValue getRemoteValue() {
        return remoteValue;
    }

    /** Try to extract WDHandle if this value is a remote object (may be null). */
    private WDHandle getHandle() {
        try {
            return (WDHandle) remoteValue.getClass().getMethod("getHandle").invoke(remoteValue);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ElementHandle asElement() {
        checkDisposed();
        if ("node".equals(remoteValue.getType())) {
            // Keep legacy probe; run arrow with 'this' bound to the element
            Boolean isElement = Boolean.TRUE.equals(evaluate("(obj) => obj instanceof Element", null));
            if (isElement != null && isElement.booleanValue()) {
                return new ElementHandleImpl(webDriver, remoteValue, target);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        WDHandle handle = getHandle();
        if (handle != null) {
            webDriver.script().disown(Collections.singletonList(handle), target);
        }
        disposed = true;
    }

    /**
     * Evaluate JS with this-handle bound as `this`.
     * If `arg` is a WDRemoteValue, treat it as the `this` binding (do not pass it as a local arg).
     */
    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();

        // Decide 'this' binding: default to this.remoteValue; override if arg is a WDRemoteValue
        WDRemoteReference.SharedReference thisRef = ScriptUtils.sharedRef(remoteValue);
        List<WDLocalValue> argsList = Collections.emptyList();

        if (arg instanceof WDRemoteValue) {
            thisRef = ScriptUtils.sharedRef((WDRemoteValue) arg);
        } else if (arg != null) {
            argsList = Collections.singletonList(WDLocalValue.fromObject(arg));
        }

        // Normalize expression into a function that uses `this`
        String fn = normalizeToFunction(expression, /*forHandle*/ false);

        WDEvaluateResult result = webDriver.script().callFunction(
                fn,
                /* awaitPromise */ true,
                target,
                argsList,
                thisRef,
                WDResultOwnership.NONE,
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue resultValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();

            // If result is a remote handle, disown and return null (mirror old behavior)
            WDHandle handle = null;
            try {
                handle = (WDHandle) resultValue.getClass().getMethod("getHandle").invoke(resultValue);
            } catch (Exception ignore) { /* no handle → serializable */ }

            if (handle != null) {
                webDriver.script().disown(Collections.singletonList(handle), target);
                return null;
            }
            return convertRemoteValue(resultValue);
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            WDExceptionDetails error = ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails();
            throw new PlaywrightException("Evaluation failed: " + error.getText());
        }
        return null;
    }

    /**
     * Evaluate and return a JSHandle (keep handle ownership).
     */
    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        checkDisposed();

        WDRemoteReference.SharedReference thisRef = ScriptUtils.sharedRef(remoteValue);
        List<WDLocalValue> argsList = Collections.emptyList();

        if (arg instanceof WDRemoteValue) {
            thisRef = ScriptUtils.sharedRef((WDRemoteValue) arg);
        } else if (arg != null) {
            argsList = Collections.singletonList(WDLocalValue.fromObject(arg));
        }

        String fn = normalizeToFunction(expression, /*forHandle*/ true);

        WDEvaluateResult result = webDriver.script().callFunction(
                fn,
                /* awaitPromise */ true,
                target,
                argsList,
                thisRef,
                WDResultOwnership.ROOT, // ensure we get a handle back for objects
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue resultValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
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
        Object keysObj = evaluate("(obj) => Object.keys(obj)", null);
        if (!(keysObj instanceof List)) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) keysObj;
        Map<String, JSHandle> properties = new HashMap<String, JSHandle>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            JSHandle propHandle = getProperty(key);
            properties.put(key, propHandle);
        }
        return properties;
    }

    @Override
    public JSHandle getProperty(String propertyName) {
        checkDisposed();
        String script = "(obj, key) => obj[key]";
        return evaluateHandle(script, propertyName);
    }

    @Override
    public Object jsonValue() {
        checkDisposed();
        WDHandle handle = getHandle();
        if (handle != null) {
            return new HashMap<String, Object>();
        }
        return convertRemoteValue(remoteValue);
    }

    @Override
    public String toString() {
        String info;
        WDHandle handle = getHandle();
        if (handle != null) {
            info = "handle=" + handle.value();
        } else {
            info = "value=" + convertRemoteValue(remoteValue);
        }
        return "JSHandle@" + target + "{" + info + "}";
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private void checkDisposed() {
        if (disposed) {
            throw new IllegalStateException("JSHandle has been disposed.");
        }
    }

    /**
     * Convert WDRemoteValue recursively to plain Java objects.
     * Treat non-serializable remote handles as empty objects.
     */
    Object convertRemoteValue(WDRemoteValue rv) {
        if (rv == null) return null;

        String type = rv.getType();
        if ("undefined".equals(type) || "null".equals(type)) return null;

        if ("boolean".equals(type)) {
            return ((WDPrimitiveProtocolValue.BooleanValue) rv).getValue();
        }
        if ("number".equals(type)) {
            return ((WDPrimitiveProtocolValue.NumberValue) rv).getValue();
        }
        if ("string".equals(type)) {
            return ((WDPrimitiveProtocolValue.StringValue) rv).getValue();
        }
        if ("bigint".equals(type)) {
            String bigIntStr = ((WDPrimitiveProtocolValue.BigIntValue) rv).getValue();
            return new java.math.BigInteger(bigIntStr);
        }
        if ("array".equals(type)) {
            List<WDRemoteValue> listVal = ((WDRemoteValue.ArrayRemoteValue) rv).getValue();
            List<Object> out = new ArrayList<Object>(listVal.size());
            for (int i = 0; i < listVal.size(); i++) {
                out.add(convertRemoteValue(listVal.get(i)));
            }
            return out;
        }
        if ("object".equals(type)) {
            Map<WDRemoteValue, WDRemoteValue> objMap = ((WDRemoteValue.ObjectRemoteValue) rv).getValue();
            Map<String, Object> resultMap = new HashMap<String, Object>();
            for (Map.Entry<WDRemoteValue, WDRemoteValue> e : objMap.entrySet()) {
                String key = (String) convertRemoteValue(e.getKey());
                Object val = convertRemoteValue(e.getValue());
                resultMap.put(key, val);
            }
            return resultMap;
        }
        if ("date".equals(type)) {
            return ((WDRemoteValue.DateRemoteValue) rv).getValue();
        }
        if ("regexp".equals(type)) {
            return ((WDRemoteValue.RegExpRemoteValue) rv).getValue().toString();
        }

        // Fallback for non-serializable remote objects: return {}
        try {
            WDHandle rvHandle = (WDHandle) rv.getClass().getMethod("getHandle").invoke(rv);
            if (rvHandle != null) {
                return new HashMap<String, Object>();
            }
        } catch (Exception ignore) { /* no handle method */ }

        return new HashMap<String, Object>();
    }

    /**
     * Normalize an arbitrary expression into a function declaration using `this`.
     * - If it's already a function: call it as-is.
     * - If it's an arrow: call it with (this, ...(arguments||[])).
     * - If it looks like a statement/this.*: wrap into function(){ return <expr>; }.
     * - If it's a simple property name: wrap into function(){ return this.<prop>; }.
     */
    private String normalizeToFunction(String expression, boolean forHandle) {
        String trimmed = expression == null ? "" : expression.trim();

        // Treat genuine function declarations
        if (WebDriverUtil.isFunctionExpression(trimmed) || trimmed.startsWith("function")) {
            return trimmed;
        }

        // Treat arrow functions
        if (trimmed.contains("=>")) {
            return "function(){ return (" + trimmed + ")(this, ...(arguments||[])); }";
        }

        // Statements or explicit this.* or calls: wrap and return value
        if (trimmed.startsWith("this.") || trimmed.endsWith(")") || trimmed.contains(";")) {
            return "function(){ return " + trimmed + "; }";
        }

        // Fallback: simple property access on `this`
        return "function(){ return this." + trimmed + "; }";
    }
}
