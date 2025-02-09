package wd4j.impl.webdriver.type.script;

import java.util.List;
import java.util.Map;

public abstract class RemoteValue {
    private final String type;
    private final Handle handle;
    private final InternalId internalId;

    protected RemoteValue(String type, Handle handle, InternalId internalId) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
        this.handle = handle;
        this.internalId = internalId;
    }

    public String getType() {
        return type;
    }

    public Handle getHandle() {
        return handle;
    }

    public InternalId getInternalId() {
        return internalId;
    }

    class SymbolRemoteValue extends RemoteValue {
        public SymbolRemoteValue(Handle handle, InternalId internalId) {
            super("symbol", handle, internalId);
        }
    }

    class ArrayRemoteValue extends RemoteValue {
        private final List<RemoteValue> value;

        public ArrayRemoteValue(Handle handle, InternalId internalId, List<RemoteValue> value) {
            super("array", handle, internalId);
            this.value = value;
        }

        public List<RemoteValue> getValue() {
            return value;
        }
    }

    class ObjectRemoteValue extends RemoteValue {
        private final Map<RemoteValue, RemoteValue> value;

        public ObjectRemoteValue(Handle handle, InternalId internalId, Map<RemoteValue, RemoteValue> value) {
            super("object", handle, internalId);
            this.value = value;
        }

        public Map<RemoteValue, RemoteValue> getValue() {
            return value;
        }
    }

    class FunctionRemoteValue extends RemoteValue {
        public FunctionRemoteValue(Handle handle, InternalId internalId) {
            super("function", handle, internalId);
        }
    }

    class RegExpRemoteValue extends RemoteValue {
        public RegExpRemoteValue(Handle handle, InternalId internalId) {
            super("regexp", handle, internalId);
        }
    }

    class DateRemoteValue extends RemoteValue {
        public DateRemoteValue(Handle handle, InternalId internalId) {
            super("date", handle, internalId);
        }
    }

    class MapRemoteValue extends RemoteValue {
        private final Map<RemoteValue, RemoteValue> value;

        public MapRemoteValue(Handle handle, InternalId internalId, Map<RemoteValue, RemoteValue> value) {
            super("map", handle, internalId);
            this.value = value;
        }

        public Map<RemoteValue, RemoteValue> getValue() {
            return value;
        }
    }

    class SetRemoteValue extends RemoteValue {
        private final List<RemoteValue> value;

        public SetRemoteValue(Handle handle, InternalId internalId, List<RemoteValue> value) {
            super("set", handle, internalId);
            this.value = value;
        }

        public List<RemoteValue> getValue() {
            return value;
        }
    }

    class WeakMapRemoteValue extends RemoteValue {
        public WeakMapRemoteValue(Handle handle, InternalId internalId) {
            super("weakmap", handle, internalId);
        }
    }

    class WeakSetRemoteValue extends RemoteValue {
        public WeakSetRemoteValue(Handle handle, InternalId internalId) {
            super("weakset", handle, internalId);
        }
    }

    class GeneratorRemoteValue extends RemoteValue {
        public GeneratorRemoteValue(Handle handle, InternalId internalId) {
            super("generator", handle, internalId);
        }
    }

    class ErrorRemoteValue extends RemoteValue {
        public ErrorRemoteValue(Handle handle, InternalId internalId) {
            super("error", handle, internalId);
        }
    }

    class ProxyRemoteValue extends RemoteValue {
        public ProxyRemoteValue(Handle handle, InternalId internalId) {
            super("proxy", handle, internalId);
        }
    }

    class PromiseRemoteValue extends RemoteValue {
        public PromiseRemoteValue(Handle handle, InternalId internalId) {
            super("promise", handle, internalId);
        }
    }

    class TypedArrayRemoteValue extends RemoteValue {
        public TypedArrayRemoteValue(Handle handle, InternalId internalId) {
            super("typedarray", handle, internalId);
        }
    }

    class ArrayBufferRemoteValue extends RemoteValue {
        public ArrayBufferRemoteValue(Handle handle, InternalId internalId) {
            super("arraybuffer", handle, internalId);
        }
    }

    class NodeListRemoteValue extends RemoteValue {
        private final List<RemoteValue> value;

        public NodeListRemoteValue(Handle handle, InternalId internalId, List<RemoteValue> value) {
            super("nodelist", handle, internalId);
            this.value = value;
        }

        public List<RemoteValue> getValue() {
            return value;
        }
    }

    class HTMLCollectionRemoteValue extends RemoteValue {
        private final List<RemoteValue> value;

        public HTMLCollectionRemoteValue(Handle handle, InternalId internalId, List<RemoteValue> value) {
            super("htmlcollection", handle, internalId);
            this.value = value;
        }

        public List<RemoteValue> getValue() {
            return value;
        }
    }

    public class NodeRemoteValue extends RemoteValue {
        private final SharedId sharedId;
        private final NodeProperties value;

        public NodeRemoteValue(Handle handle, InternalId internalId, SharedId sharedId, NodeProperties value) {
            super("node", handle, internalId);
            this.sharedId = sharedId;
            this.value = value;
        }

        public SharedId getSharedId() {
            return sharedId;
        }

        public NodeProperties getValue() {
            return value;
        }
    }

    class WindowProxyRemoteValue extends RemoteValue {
        WindowProxyProperties value;

        public WindowProxyRemoteValue(Handle handle, WindowProxyProperties value, InternalId internalId) {
            super("window", handle, internalId);
            this.value = value;
        }

        public WindowProxyProperties getValue() {
            return value;
        }
    }
}
