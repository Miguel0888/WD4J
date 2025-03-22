package wd4j.impl.playwright;

import wd4j.api.ElementHandle;
import wd4j.api.JSHandle;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.dto.type.script.*;

import java.util.*;
import java.util.stream.Collectors;

public class JSHandleImpl implements JSHandle {
    private final WDScriptManager scriptManager;
    private final WDHandle handle;
    private final WDRealm realm;
    private boolean disposed = false;

    public JSHandleImpl(WDHandle handle, WDRealm realm) {
        this.scriptManager = null; // ToDo: Implement this, how to get the script manager? Might be a constructor parameter?
        this.handle = handle;
        this.realm = realm;
    }

    @Override
    public ElementHandle asElement() {
        if (isElementHandle()) {
            return new ElementHandleImpl(handle, realm);
        }
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        scriptManager.disown(Collections.singletonList(handle), new WDTarget.RealmTarget(realm));
        disposed = true;
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();
        WDTarget target = new WDTarget.RealmTarget(realm);
        WDEvaluateResult result = scriptManager.evaluate(expression, target, true);
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
                ", realm=" + realm.value() +
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
        WDEvaluateResult evaluate = scriptManager.evaluate("obj => Object.entries(obj)", new WDTarget.RealmTarget(realm), true);
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
            return new JSHandleImpl(new WDHandle(localValue.toString()), realm);
        }

        throw new IllegalArgumentException("Unsupported WDLocalValue type: " + localValue.getClass().getSimpleName());
    }
}
