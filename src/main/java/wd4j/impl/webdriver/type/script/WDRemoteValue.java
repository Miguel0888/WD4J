package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@JsonAdapter(WDRemoteValue.WDRemoteValueAdapter.class) // 🔥 Automatische JSON-Serialisierung und -Deserialisierung
public abstract class WDRemoteValue {
    private final String type;
    private final WDHandle handle;
    private final WDInternalId internalId;

    // 🔥 **JSON Adapter für automatische (De)Serialisierung**
    public static class WDRemoteValueAdapter implements JsonSerializer<WDRemoteValue>, JsonDeserializer<WDRemoteValue> {
        @Override
        public WDRemoteValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in WDRemoteValue JSON");
            }

            String type = jsonObject.get("type").getAsString();

            // ✅ **Falls der Typ ein PrimitiveProtocolValue ist, verwenden wir den bestehenden Adapter**
            if (EnumWrapper.contains(WDPrimitiveProtocolValue.Type.class, type)) {
                return context.deserialize(jsonObject, WDPrimitiveProtocolValue.class);
            }

            switch (type) {
                case "symbol":
                    return context.deserialize(jsonObject, SymbolRemoteValue.class);
                case "array":
                    return context.deserialize(jsonObject, ArrayRemoteValue.class);
                case "object":
                    return context.deserialize(jsonObject, ObjectRemoteValue.class);
                case "function":
                    return context.deserialize(jsonObject, FunctionRemoteValue.class);
                case "regexp":
                    return mergeWithLocalValue(context.deserialize(jsonObject, WDLocalValue.RegExpLocalValue.class), jsonObject);
                case "date":
                    return mergeWithLocalValue(context.deserialize(jsonObject, WDLocalValue.DateLocalValue.class), jsonObject);
                case "map":
                    return context.deserialize(jsonObject, MapRemoteValue.class);
                case "set":
                    return context.deserialize(jsonObject, SetRemoteValue.class);
                case "weakmap":
                    return context.deserialize(jsonObject, WeakMapRemoteValue.class);
                case "weakset":
                    return context.deserialize(jsonObject, WeakSetRemoteValue.class);
                case "generator":
                    return context.deserialize(jsonObject, GeneratorRemoteValue.class);
                case "error":
                    return context.deserialize(jsonObject, ErrorRemoteValue.class);
                case "proxy":
                    return context.deserialize(jsonObject, ProxyRemoteValue.class);
                case "promise":
                    return context.deserialize(jsonObject, PromiseRemoteValue.class);
                case "typedarray":
                    return context.deserialize(jsonObject, TypedArrayRemoteValue.class);
                case "arraybuffer":
                    return context.deserialize(jsonObject, ArrayBufferRemoteValue.class);
                case "nodelist":
                    return context.deserialize(jsonObject, NodeListRemoteValue.class);
                case "htmlcollection":
                    return context.deserialize(jsonObject, HTMLCollectionRemoteValue.class);
                case "node":
                    return context.deserialize(jsonObject, NodeRemoteValue.class);
                case "window":
                    return context.deserialize(jsonObject, WindowProxyRemoteValue.class);
                default:
                    throw new JsonParseException("Unknown WDRemoteValue type: " + type);
            }
        }

        // ✅ **Serialisierung: Wandelt `WDRemoteValue` in JSON um**
        @Override
        public JsonElement serialize(WDRemoteValue src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = (JsonObject) context.serialize(src);

            // Falls `src` ein PrimitiveProtocolValue ist, nutzen wir den anderen Adapter
            if (src instanceof WDPrimitiveProtocolValue) {
                return context.serialize(src, WDPrimitiveProtocolValue.class);
            }

            return jsonObject;
        }

        // ✅ **Hilfsmethode zur Kombination von Remote- und Local-Werten**
        private WDRemoteValue mergeWithLocalValue(WDLocalValue<?> localValue, JsonObject jsonObject) {
            WDHandle handle = jsonObject.has("handle") ? new WDHandle(jsonObject.get("handle").getAsString()) : null;
            WDInternalId internalId = jsonObject.has("internalId") ? new WDInternalId(jsonObject.get("internalId").getAsString()) : null;

            if (localValue instanceof WDLocalValue.RegExpLocalValue) {
                return new RegExpRemoteValue(handle, internalId, ((WDLocalValue.RegExpLocalValue) localValue).getValue());
            } else if (localValue instanceof WDLocalValue.DateLocalValue) {
                return new DateRemoteValue(handle, internalId, ((WDLocalValue.DateLocalValue) localValue).getValue());
            }

            throw new JsonParseException("Unexpected local value type in RemoteValue merging.");
        }
    }

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

    // Standard-Implementierung: Nicht unterstützte Typen werfen Exception
    public String asString() {
        throw new UnsupportedOperationException(
                "Cannot convert WDRemoteValue of type '" + type + "' to String."
        );
    }

    static class SymbolRemoteValue extends WDRemoteValue {
        public SymbolRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("symbol", handle, internalId);
        }
    }

    static class ArrayRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public ArrayRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("array", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    static class ObjectRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public ObjectRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("object", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    static class FunctionRemoteValue extends WDRemoteValue {
        public FunctionRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("function", handle, internalId);
        }
    }

    static class RegExpRemoteValue extends WDRemoteValue {
        private final WDLocalValue.RegExpLocalValue.RegExpValue value;

        public RegExpRemoteValue(WDHandle handle, WDInternalId internalId, WDLocalValue.RegExpLocalValue.RegExpValue value) {
            super("regexp", handle, internalId);
            this.value = value;
        }

        public WDLocalValue.RegExpLocalValue.RegExpValue getValue() {
            return value;
        }

        @Override
        public String asString() {
            return "/" + value.getPattern() + "/" + (value.getFlags() != null ? value.getFlags() : "");
        }
    }

    static class DateRemoteValue extends WDRemoteValue {
        private final String value; // DateLocalValue's value

        public DateRemoteValue(WDHandle handle, WDInternalId internalId, String value) {
            super("date", handle, internalId);
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String asString() {
            return value;
        }
    }

    static class MapRemoteValue extends WDRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public MapRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("map", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    static class SetRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public SetRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("set", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    static class WeakMapRemoteValue extends WDRemoteValue {
        public WeakMapRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakmap", handle, internalId);
        }
    }

    static class WeakSetRemoteValue extends WDRemoteValue {
        public WeakSetRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakset", handle, internalId);
        }
    }

    static class GeneratorRemoteValue extends WDRemoteValue {
        public GeneratorRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("generator", handle, internalId);
        }
    }

    static class ErrorRemoteValue extends WDRemoteValue {
        public ErrorRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("error", handle, internalId);
        }
    }

    static class ProxyRemoteValue extends WDRemoteValue {
        public ProxyRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("proxy", handle, internalId);
        }
    }

    static class PromiseRemoteValue extends WDRemoteValue {
        public PromiseRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("promise", handle, internalId);
        }
    }

    static class TypedArrayRemoteValue extends WDRemoteValue {
        public TypedArrayRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("typedarray", handle, internalId);
        }
    }

    static class ArrayBufferRemoteValue extends WDRemoteValue {
        public ArrayBufferRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("arraybuffer", handle, internalId);
        }
    }

    static class NodeListRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public NodeListRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("nodelist", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }

        @Override
        public String asString() {
            return value.stream()
                    .map(WDRemoteValue::asString)
                    .reduce("", String::concat);
        }
    }

    static class HTMLCollectionRemoteValue extends WDRemoteValue {
        private final List<WDRemoteValue> value;

        public HTMLCollectionRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("htmlcollection", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }

        @Override
        public String asString() {
            return value.stream()
                    .map(WDRemoteValue::asString)
                    .reduce("", String::concat);
        }
    }

    public static class NodeRemoteValue extends WDRemoteValue {
        private final WDSharedId WDSharedId;
        private final NodeProperties value;

        public NodeRemoteValue(WDHandle handle, WDInternalId internalId, WDSharedId WDSharedId, NodeProperties value) {
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

        @Override
        public String asString() {
            return value.getNodeValue() != null ? value.getNodeValue() : "";
        }
    }

    static class WindowProxyRemoteValue extends WDRemoteValue {
        WindowProxyProperties value;

        public WindowProxyRemoteValue(WDHandle handle, WindowProxyProperties value, WDInternalId internalId) {
            super("window", handle, internalId);
            this.value = value;
        }

        public WindowProxyProperties getValue() {
            return value;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class NodeProperties {
        private final long nodeType;
        private final long childNodeCount;
        private final Map<String, String> attributes; // Optional
        private final List<NodeRemoteValue> children; // Optional
        private final String localName; // Optional
        private final Mode mode; // "open" or "closed" (Optional)
        private final String namespaceURI; // Optional
        private final String nodeValue; // Optional
        private final NodeRemoteValue shadowRoot; // Optional

        public NodeProperties(
                long nodeType, long childNodeCount,
                Map<String, String> attributes, List<NodeRemoteValue> children,
                String localName, Mode mode, String namespaceURI,
                String nodeValue, NodeRemoteValue shadowRoot) {
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

        public long getNodeType() {
            return nodeType;
        }

        public long getChildNodeCount() {
            return childNodeCount;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public List<NodeRemoteValue> getChildren() {
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

        public NodeRemoteValue getShadowRoot() {
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
        private final WDBrowsingContext browsingContextRequest;

        public WindowProxyProperties(WDBrowsingContext browsingContext) {
            this.browsingContextRequest = browsingContext;
        }

        public WDBrowsingContext getBrowsingContext() {
            return browsingContextRequest;
        }
    }
}
