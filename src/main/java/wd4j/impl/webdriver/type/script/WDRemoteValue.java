package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.mapping.EnumWrapper;

import java.util.List;
import java.util.Map;

public abstract class WDRemoteValue {
    private final String type;
    private final WDHandle WDHandle;
    private final WDInternalId WDInternalId;

    protected WDRemoteValue(String type, WDHandle WDHandle, WDInternalId WDInternalId) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
        this.WDHandle = WDHandle;
        this.WDInternalId = WDInternalId;
    }

    public String getType() {
        return type;
    }

    public WDHandle getHandle() {
        return WDHandle;
    }

    public WDInternalId getInternalId() {
        return WDInternalId;
    }

    class SymbolWDRemoteValue extends WDRemoteValue {
        public SymbolWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("symbol", WDHandle, WDInternalId);
        }
    }

    class ArrayWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public ArrayWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, List<WDRemoteValue> value) {
            super("array", WDHandle, WDInternalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class ObjectWDRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public ObjectWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("object", WDHandle, WDInternalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class FunctionWDRemoteValue extends WDRemoteValue {
        public FunctionWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("function", WDHandle, WDInternalId);
        }
    }

    class RegExpWDRemoteValue extends WDRemoteValue {
        public RegExpWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("regexp", WDHandle, WDInternalId);
        }
    }

    class DateWDRemoteValue extends WDRemoteValue {
        public DateWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("date", WDHandle, WDInternalId);
        }
    }

    class MapWDRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public MapWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("map", WDHandle, WDInternalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class SetWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public SetWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, List<WDRemoteValue> value) {
            super("set", WDHandle, WDInternalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class WeakMapWDRemoteValue extends WDRemoteValue {
        public WeakMapWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("weakmap", WDHandle, WDInternalId);
        }
    }

    class WeakSetWDRemoteValue extends WDRemoteValue {
        public WeakSetWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("weakset", WDHandle, WDInternalId);
        }
    }

    class GeneratorWDRemoteValue extends WDRemoteValue {
        public GeneratorWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("generator", WDHandle, WDInternalId);
        }
    }

    class ErrorWDRemoteValue extends WDRemoteValue {
        public ErrorWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("error", WDHandle, WDInternalId);
        }
    }

    class ProxyWDRemoteValue extends WDRemoteValue {
        public ProxyWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("proxy", WDHandle, WDInternalId);
        }
    }

    class PromiseWDRemoteValue extends WDRemoteValue {
        public PromiseWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("promise", WDHandle, WDInternalId);
        }
    }

    class TypedArrayWDRemoteValue extends WDRemoteValue {
        public TypedArrayWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("typedarray", WDHandle, WDInternalId);
        }
    }

    class ArrayBufferWDRemoteValue extends WDRemoteValue {
        public ArrayBufferWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId) {
            super("arraybuffer", WDHandle, WDInternalId);
        }
    }

    class NodeListWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public NodeListWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, List<WDRemoteValue> value) {
            super("nodelist", WDHandle, WDInternalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class HTMLCollectionWDRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public HTMLCollectionWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, List<WDRemoteValue> value) {
            super("htmlcollection", WDHandle, WDInternalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    public class NodeWDRemoteValue extends WDRemoteValue {
        private final WDSharedId WDSharedId;
        private final NodeProperties value;

        public NodeWDRemoteValue(WDHandle WDHandle, WDInternalId WDInternalId, WDSharedId WDSharedId, NodeProperties value) {
            super("node", WDHandle, WDInternalId);
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

        public WindowProxyWDRemoteValue(WDHandle WDHandle, WindowProxyProperties value, WDInternalId WDInternalId) {
            super("window", WDHandle, WDInternalId);
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
        private final BrowsingContextRequest browsingContextRequest;

        public WindowProxyProperties(BrowsingContextRequest browsingContextRequest) {
            this.browsingContextRequest = browsingContextRequest;
        }

        public BrowsingContextRequest getBrowsingContext() {
            return browsingContextRequest;
        }
    }
}
