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
    protected final WDHandle handle;
    protected final WDTarget target; // can be RealmTarget or ContextTarget (to avoid conversion to RealmTarget when not necessary)
    protected boolean disposed = false;

    public JSHandleImpl(WebDriver webDriver, WDHandle handle, WDTarget target) {
        this.webDriver = webDriver;
        this.handle = handle;
        this.target = target;
    }

    @Override
    public ElementHandle asElement() {
        if (isElementHandle()) {
            return new ElementHandleImpl(webDriver, handle, target);
        }
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        webDriver.script().disown(Collections.singletonList(handle), target);
        disposed = true;
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();
        WDEvaluateResult result = webDriver.script().evaluate(expression, target, true);
        if(result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            return convertWDLocalValue(((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult());
        }
        return null;
    }

    private Object convertWDLocalValue(WDRemoteValue result) {
        return null; // ToDo: Implement this
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
                "handle=" + handle.value() +
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

    private Object convertWDLocalValue(WDLocalValue localValue) {
        if (localValue == null) return null;

        // 🔹 Fallunterscheidung für konkrete Implementierungen
        if (localValue instanceof WDPrimitiveProtocolValue.StringValue) {
            return ((WDPrimitiveProtocolValue.StringValue) localValue).getValue();
        }
        if (localValue instanceof WDPrimitiveProtocolValue.NumberValue) {
            return ((WDPrimitiveProtocolValue.NumberValue) localValue).getValue();
        }
        if (localValue instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) localValue).getValue();
        }
        if (localValue instanceof WDPrimitiveProtocolValue.NullValue) {
            return null;
        }
        if (localValue instanceof WDLocalValue.ArrayLocalValue) {
            return ((WDLocalValue.ArrayLocalValue) localValue).getValue().stream()
                    .map(this::convertWDLocalValue)
                    .collect(Collectors.toList());
        }
        if (localValue instanceof WDLocalValue.ObjectLocalValue) {
            return extractProperties();
        }
        if (localValue instanceof WDLocalValue.DateLocalValue) {
            return ((WDLocalValue.DateLocalValue) localValue).getValue();
        }
        if (localValue instanceof WDLocalValue.MapLocalValue) {
            return ((WDLocalValue.MapLocalValue) localValue).getValue().entrySet().stream()
                    .collect(Collectors.toMap(e -> convertWDLocalValue(e.getKey()), e -> convertWDLocalValue(e.getValue())));
        }
        if (localValue instanceof WDLocalValue.SetLocalValue) {
            return ((WDLocalValue.SetLocalValue) localValue).getValue().stream()
                    .map(this::convertWDLocalValue)
                    .collect(Collectors.toSet());
        }
        if (localValue instanceof WDLocalValue.RegExpLocalValue) {
            return ((WDLocalValue.RegExpLocalValue) localValue).getValue().getPattern();
        }
        if (localValue instanceof WDRemoteReference.SharedReference || localValue instanceof WDRemoteReference.RemoteObjectReference) {
            return new JSHandleImpl(webDriver, new WDHandle(localValue.toString()), target);
        }

        throw new IllegalArgumentException("Unsupported WDLocalValue type: " + localValue.getClass().getSimpleName());
    }
}
