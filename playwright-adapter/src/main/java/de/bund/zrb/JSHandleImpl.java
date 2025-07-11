package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.JSHandle;
import de.bund.zrb.type.script.*;

import java.util.*;

public class JSHandleImpl implements JSHandle {
    protected final WebDriver webDriver;
    protected final WDRemoteReference<?> remoteReference;
    protected final WDTarget target;
    protected boolean disposed = false;

    public JSHandleImpl(WebDriver webDriver, WDRemoteReference<?> remoteReference, WDTarget target) {
        this.webDriver = webDriver;
        this.remoteReference = remoteReference;
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
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        if (remoteReference instanceof WDRemoteReference.RemoteObjectReference) {
            webDriver.script().disown(Collections.singletonList(getHandle()), target);
        }
        disposed = true;
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        checkDisposed();

        List<WDLocalValue> args = new ArrayList<>();
        if (arg != null) {
            args.add(WDLocalValue.fromObject(arg));
        }

        WDEvaluateResult result = webDriver.script().callFunction(
                expression,
                true,
                target,
                args,
                remoteReference,
                WDResultOwnership.ROOT,
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remote = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remote instanceof WDPrimitiveProtocolValue) {
                return unwrapPrimitive((WDPrimitiveProtocolValue) remote);
            }
            return remote;
        }

        throw new RuntimeException("evaluate failed");
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        checkDisposed();

        List<WDLocalValue> args = new ArrayList<>();
        if (arg != null) {
            args.add(WDLocalValue.fromObject(arg));
        }

        WDEvaluateResult result = webDriver.script().callFunction(
                expression,
                true,
                target,
                args,
                remoteReference,
                WDResultOwnership.ROOT,
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remote = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remote instanceof WDRemoteReference.SharedReference) {
                return new JSHandleImpl(webDriver, (WDRemoteReference.SharedReference) remote, target);
            }
            if (remote instanceof WDRemoteReference.RemoteObjectReference) {
                return new JSHandleImpl(webDriver, (WDRemoteReference.RemoteObjectReference) remote, target);
            }
            throw new RuntimeException("evaluateHandle: Unexpected primitive. Use evaluate() instead.");
        }

        throw new RuntimeException("evaluateHandle failed");
    }

    @Override
    public Object jsonValue() {
        checkDisposed();

        WDEvaluateResult result = webDriver.script().callFunction(
                "obj => obj",
                true,
                target,
                Collections.emptyList(),
                remoteReference,
                WDResultOwnership.ROOT,
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remote = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remote instanceof WDPrimitiveProtocolValue) {
                return unwrapPrimitive((WDPrimitiveProtocolValue) remote);
            }
            return remote;
        }

        throw new RuntimeException("jsonValue failed");
    }

    private Object unwrapPrimitive(WDPrimitiveProtocolValue primitive) {
        if (primitive instanceof WDPrimitiveProtocolValue.StringValue) {
            return ((WDPrimitiveProtocolValue.StringValue) primitive).getValue();
        }
        if (primitive instanceof WDPrimitiveProtocolValue.BooleanValue) {
            return ((WDPrimitiveProtocolValue.BooleanValue) primitive).getValue();
        }
        if (primitive instanceof WDPrimitiveProtocolValue.NumberValue) {
            return ((WDPrimitiveProtocolValue.NumberValue) primitive).asObject();
        }
        if (primitive instanceof WDPrimitiveProtocolValue.NullValue) {
            return null;
        }
        if (primitive instanceof WDPrimitiveProtocolValue.UndefinedValue) {
            return null;
        }
        if (primitive instanceof WDPrimitiveProtocolValue.BigIntValue) {
            return ((WDPrimitiveProtocolValue.BigIntValue) primitive).getValue();
        }
        throw new RuntimeException("Unknown primitive type: " + primitive);
    }

    @Override
    public Map<String, JSHandle> getProperties() {
        throw new UnsupportedOperationException("getProperties not implemented yet.");
    }

    @Override
    public JSHandle getProperty(String propertyName) {
        throw new UnsupportedOperationException("getProperty not implemented yet.");
    }

    private void checkDisposed() {
        if (disposed) {
            throw new IllegalStateException("JSHandle has been disposed.");
        }
    }

    @Override
    public String toString() {
        return "JSHandleImpl{" +
                "remoteReference=" + remoteReference +
                ", target=" + target +
                '}';
    }
}
