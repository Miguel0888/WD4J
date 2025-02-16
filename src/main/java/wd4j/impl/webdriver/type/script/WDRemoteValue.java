package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;
import wd4j.impl.webdriver.mapping.EnumWrapper;

import java.util.List;
import java.util.Map;

public abstract class WDRemoteValue {
    private final String type;
    private final WDHandle handle;
    private final WDInternalId internalId;

    protected WDRemoteValue(String type, WDHandle handle, WDInternalId internalId) {
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

    public WDHandle getHandle() {
        return handle;
    }

    public WDInternalId getInternalId() {
        return internalId;
    }

    class SymbolWDRemoteValue extends WDRemoteValue {
        public SymbolWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("symbol", handle, internalId);
        }
    }

    class ArrayWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public ArrayWDRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("array", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class ObjectWDRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public ObjectWDRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("object", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class FunctionWDRemoteValue extends WDRemoteValue {
        public FunctionWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("function", handle, internalId);
        }
    }

    class RegExpWDRemoteValue extends WDRemoteValue {
        public RegExpWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("regexp", handle, internalId);
        }
    }

    class DateWDRemoteValue extends WDRemoteValue {
        public DateWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("date", handle, internalId);
        }
    }

    class MapWDRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public MapWDRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("map", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class SetWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public SetWDRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("set", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class WeakMapWDRemoteValue extends WDRemoteValue {
        public WeakMapWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakmap", handle, internalId);
        }
    }

    class WeakSetWDRemoteValue extends WDRemoteValue {
        public WeakSetWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakset", handle, internalId);
        }
    }

    class GeneratorWDRemoteValue extends WDRemoteValue {
        public GeneratorWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("generator", handle, internalId);
        }
    }

    class ErrorWDRemoteValue extends WDRemoteValue {
        public ErrorWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("error", handle, internalId);
        }
    }

    class ProxyWDRemoteValue extends WDRemoteValue {
        public ProxyWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("proxy", handle, internalId);
        }
    }

    class PromiseWDRemoteValue extends WDRemoteValue {
        public PromiseWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("promise", handle, internalId);
        }
    }

    class TypedArrayWDRemoteValue extends WDRemoteValue {
        public TypedArrayWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("typedarray", handle, internalId);
        }
    }

    class ArrayBufferWDRemoteValue extends WDRemoteValue {
        public ArrayBufferWDRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("arraybuffer", handle, internalId);
        }
    }

    class NodeListWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public NodeListWDRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("nodelist", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class HTMLCollectionWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public HTMLCollectionWDRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("htmlcollection", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    public class NodeWDRemoteValue extends WDRemoteValue {
        private final WDSharedId WDSharedId;
        private final NodeProperties value;

        public NodeWDRemoteValue(WDHandle handle, WDInternalId internalId, WDSharedId WDSharedId, NodeProperties value) {
            super("node", handle, internalId);
            this.WDSharedId = WDSharedId;
            this.value = value;
        }

        public WDSharedId getSharedId() {
            return WDSharedId;
        }

        public NodeProperties getValue() {
            return value;
        }
    }

    class WindowProxyWDRemoteValue extends WDRemoteValue {
        WindowProxyProperties value;

        public WindowProxyWDRemoteValue(WDHandle handle, WindowProxyProperties value, WDInternalId internalId) {
            super("window", handle, internalId);
            this.value = value;
        }

        public WindowProxyProperties getValue() {
            return value;
        }
    }

    public static class NodeProperties {
        private final char nodeType;
        private final char childNodeCount;
        private final Map<String, String> attributes; // Optional
        private final List<NodeWDRemoteValue> children; // Optional
        private final String localName; // Optional
        private final Mode mode; // "open" or "closed" (Optional)
        private final String namespaceURI; // Optional
        private final String nodeValue; // Optional
        private final NodeWDRemoteValue shadowRoot; // Optional

        public NodeProperties(
                char nodeType, char childNodeCount,
                Map<String, String> attributes, List<NodeWDRemoteValue> children,
                String localName, Mode mode, String namespaceURI,
                String nodeValue, NodeWDRemoteValue shadowRoot) {
            this.nodeType = nodeType;
            this.childNodeCount = childNodeCount;
            this.attributes = attributes;
            this.children = children;
            this.localName = localName;
            this.mode = mode;
            this.namespaceURI = namespaceURI;
            this.nodeValue = nodeValue;
            this.shadowRoot = shadowRoot;
        }

        public char getNodeType() {
            return nodeType;
        }

        public char getChildNodeCount() {
            return childNodeCount;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public List<NodeWDRemoteValue> getChildren() {
            return children;
        }

        public String getLocalName() {
            return localName;
        }

        public Mode getMode() {
            return mode;
        }

        public String getNamespaceURI() {
            return namespaceURI;
        }

        public String getNodeValue() {
            return nodeValue;
        }

        public NodeWDRemoteValue getShadowRoot() {
            return shadowRoot;
        }


        public enum Mode implements EnumWrapper {
            OPEN("open"),
            CLOSED("closed");

            private final String value;

            Mode(String value) {
                this.value = value;
            }

            @Override // confirmed
            public String value() {
                return value;
            }
        }
    }

    public static class WindowProxyProperties {
        private final WDBrowsingContextRequest browsingContextRequest;

        public WindowProxyProperties(WDBrowsingContextRequest WDBrowsingContextRequest) {
            this.browsingContextRequest = WDBrowsingContextRequest;
        }

        public WDBrowsingContextRequest getBrowsingContext() {
            return browsingContextRequest;
        }
    }
}
