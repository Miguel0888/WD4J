package wd4j.impl.playwright;

import wd4j.api.ElementHandle;
import wd4j.api.JSHandle;
import wd4j.impl.WebDriver;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.webdriver.type.script.*;

import java.util.*;
import java.util.stream.Collectors;

public class JSHandleImpl implements JSHandle {
    protected final WebDriver webDriver;
    protected final WDRemoteReference<?> remoteReference;
    protected final WDTarget target; // can be RealmTarget or ContextTarget (to avoid conversion to RealmTarget when not necessary)
    protected boolean disposed = false;

    public JSHandleImpl(WebDriver webDriver, WDRemoteReference<?> remoteObjectReference, WDTarget target) {
        this.webDriver = webDriver;
        this.remoteReference = remoteObjectReference;
        this.target = target;
    }

    public WDRemoteReference<?> getRemoteReference() {
        return remoteReference;
    }

    public WDHandle getHandle() {
        return ((WDRemoteReference.RemoteObjectReference) remoteReference).getHandle();
    }

    public WDTarget getTarget() {
        return target;
    }

    @Override
    public ElementHandle asElement() {

        if (remoteReference instanceof WDRemoteReference.SharedReference) {
            return new ElementHandleImpl(webDriver, (WDRemoteReference.SharedReference) remoteReference, target);
        } else if (remoteReference instanceof WDRemoteReference.RemoteObjectReference) {
            WDRemoteReference.RemoteObjectReference ror = (WDRemoteReference.RemoteObjectReference) remoteReference;
            if (ror.getSharedId() != null) {
                WDRemoteReference.SharedReference sharedRef = new WDRemoteReference.SharedReference(ror.getSharedId(), ror.getHandle());
                return new ElementHandleImpl(webDriver, sharedRef, target);
            }
        }
        // ToDo: Maybe request the missing sharedId from the browser..
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        webDriver.script().disown(Collections.singletonList(getHandle()), target);
        disposed = true;
    }

    /**
     * Evaluates the JavaScript expression in the browser context.
     *
     * @param expression JavaScript expression to be evaluated in the browser context. If the expression evaluates to a function, the function is
     * automatically invoked.
     * @param arg Optional argument to pass to {@code expression}.
     *
     * @return The corresponding Webdriver object (WDRemoteValue)
     */
    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();
        WDEvaluateResult result = webDriver.script().evaluate(expression, target, true);
        if(result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            return ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
        }
        return null;
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        // ToDo: Implement this
//        checkDisposed();
//        WDTarget target = new WDTarget.RealmTarget(realm);
//        List<WDLocalValue> args = Collections.singletonList(new WDRemoteReference.RemoteObjectReference(handle));
//        WDEvaluateResult result = scriptManager.callFunction(expression, true, target, args);
//        if(result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
//            return new JSHandleImpl(new WDHandle(((WDEvaluateResult.WDEvaluateResultSuccess)result).getResult().getHandle().value()), realm);
//        }
        return null;
    }

    @Override
    public Map<String, JSHandle> getProperties() {
        checkDisposed();
        String script = "(obj) => Object.entries(obj).reduce((acc, [key, val]) => { acc[key] = val; return acc; }, {});";
        JSHandle resultHandle = evaluateHandle(script, null);

        if (!(resultHandle instanceof JSHandleImpl)) {
            throw new RuntimeException("Unexpected type for properties retrieval.");
        }

        return ((JSHandleImpl) resultHandle).extractProperties();
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
        return evaluate("JSON.stringify", null);
    }

    @Override
    public String toString() {
        return "JSHandleImpl{" +
                "handle=" + getHandle().value() +
                // ToDo: Implement this
//                ", realm=" + target() +
                '}';
    }

    // 🔹 Hilfsmethoden

    private void checkDisposed() {
        if (disposed) {
            throw new IllegalStateException("JSHandle has been disposed.");
        }
    }

    private boolean isElementHandle() {
        String script = "(obj) => obj instanceof Element";
        return Boolean.TRUE.equals(evaluate(script, null));
    }

    private Map<String, JSHandle> extractProperties() {
        Map<String, JSHandle> properties = new HashMap<>();
        WDEvaluateResult evaluate = webDriver.script().evaluate("obj => Object.entries(obj)", target, true);
        WDRemoteValue remoteValue;
        if(evaluate instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            remoteValue = ((WDEvaluateResult.WDEvaluateResultSuccess) evaluate).getResult();
        } else {
            return properties;
        }

//        WDLocalValue<?> localValue; // ToDo: Implement this
//
//        if (localValue instanceof WDLocalValue.ObjectLocalValue) {
//            Map<WDLocalValue<?>, WDLocalValue<?>> values = ((WDLocalValue.ObjectLocalValue) localValue).getValue();
//            for (Map.Entry<WDLocalValue<?>, WDLocalValue<?>> entry : values.entrySet()) {
//                String key = convertWDLocalValue(entry.getKey()).toString();
//                JSHandle valueHandle = new JSHandleImpl(new WDHandle(entry.getValue().toString()), realm);
//                properties.put(key, valueHandle);
//            }
//        }
        return properties;
    }
}
