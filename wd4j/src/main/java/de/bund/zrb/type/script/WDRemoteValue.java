package de.bund.zrb.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WDRemoteValue is an interface to represent various types of remote values in WebDriver BiDi.
 */
@JsonAdapter(WDRemoteValue.WDRemoteValueAdapter.class)
public interface WDRemoteValue extends WDLocalValue {

    String getType();

    default String asString() {
        return "[Object: " + getType() + "]";
    }

    // JSON Adapter for automatic serialization/deserialization
    class WDRemoteValueAdapter implements JsonSerializer<WDRemoteValue>, JsonDeserializer<WDRemoteValue> {
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
                    JsonArray valueArray = jsonObject.getAsJsonArray("value");
                    Map<WDRemoteValue, WDRemoteValue> objectMap = new HashMap<>();
                    for (JsonElement entry : valueArray) {
                        JsonArray keyValuePair = entry.getAsJsonArray();
                        if (keyValuePair.size() != 2) {
                            throw new JsonParseException("Invalid object entry: Expected key-value pair.");
                        }
                        // ✅ Key als WDRemoteValue (String-Wert) umwandeln
                        WDRemoteValue key = new WDPrimitiveProtocolValue.StringValue(keyValuePair.get(0).getAsString());
                        // ✅ Value als WDRemoteValue parsen
                        WDRemoteValue value = context.deserialize(keyValuePair.get(1), WDRemoteValue.class);
                        objectMap.put(key, value);
                    }
                    return new ObjectRemoteValue(null, null, objectMap);
                case "function":
                    return context.deserialize(jsonObject, FunctionRemoteValue.class);
                case "regexp":
                    return context.deserialize(jsonObject, RegExpRemoteValue.class);
                case "date":
                    return context.deserialize(jsonObject, DateRemoteValue.class);
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
            return context.serialize(src);
            }

        private WDRemoteValue mergeWithLocalValue(WDLocalValue localValue, JsonObject jsonObject) {
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

     class BaseRemoteValue implements WDRemoteValue {
        private final String type;
        private final WDHandle handle;
        private final WDInternalId internalId;

        protected BaseRemoteValue(String type, WDHandle handle, WDInternalId internalId) {
            this.type = type;
            this.handle = handle;
            this.internalId = internalId;
        }

        @Override
        public String getType() {
            return type;
        }

        public WDHandle getHandle() {
        return handle;
    }

        public WDInternalId getInternalId() {
        return internalId;
    }
     }

    class SymbolRemoteValue extends BaseRemoteValue {
        public SymbolRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("symbol", handle, internalId);
        }
    }

    class ArrayRemoteValue extends BaseRemoteValue {
        private final List<WDRemoteValue> value;

        public ArrayRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("array", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class ObjectRemoteValue extends BaseRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public ObjectRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("object", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class FunctionRemoteValue extends BaseRemoteValue {
        public FunctionRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("function", handle, internalId);
        }
    }

    class RegExpRemoteValue extends BaseRemoteValue {
        private final WDLocalValue.RegExpLocalValue.RegExpValue value;

        public RegExpRemoteValue(WDHandle handle, WDInternalId internalId, WDLocalValue.RegExpLocalValue.RegExpValue value) {
            super("regexp", handle, internalId);
            this.value = value;
        }

        public WDLocalValue.RegExpLocalValue.RegExpValue getValue() {
            return value;
        }
    }

    class DateRemoteValue extends BaseRemoteValue {
        private final String value;

        public DateRemoteValue(WDHandle handle, WDInternalId internalId, String value) {
            super("date", handle, internalId);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    class MapRemoteValue extends BaseRemoteValue {
        private final Map<WDRemoteValue, WDRemoteValue> value;

        public MapRemoteValue(WDHandle handle, WDInternalId internalId, Map<WDRemoteValue, WDRemoteValue> value) {
            super("map", handle, internalId);
            this.value = value;
        }

        public Map<WDRemoteValue, WDRemoteValue> getValue() {
            return value;
        }
    }

    class SetRemoteValue extends BaseRemoteValue {
        private final List<WDRemoteValue> value;

        public SetRemoteValue(WDHandle handle, WDInternalId internalId, List<WDRemoteValue> value) {
            super("set", handle, internalId);
            this.value = value;
        }

        public List<WDRemoteValue> getValue() {
            return value;
        }
    }

    class WeakMapRemoteValue extends BaseRemoteValue {
        public WeakMapRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakmap", handle, internalId);
        }
    }

    class WeakSetRemoteValue extends BaseRemoteValue {
        public WeakSetRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("weakset", handle, internalId);
        }
    }

    class GeneratorRemoteValue extends BaseRemoteValue {
        public GeneratorRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("generator", handle, internalId);
        }
    }

    class ErrorRemoteValue extends BaseRemoteValue {
        public ErrorRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("error", handle, internalId);
        }
    }

    class ProxyRemoteValue extends BaseRemoteValue {
        public ProxyRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("proxy", handle, internalId);
        }
    }

    class PromiseRemoteValue extends BaseRemoteValue {
        public PromiseRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("promise", handle, internalId);
        }
    }

    class TypedArrayRemoteValue extends BaseRemoteValue {
        public TypedArrayRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("typedarray", handle, internalId);
        }
    }

    class ArrayBufferRemoteValue extends BaseRemoteValue {
        public ArrayBufferRemoteValue(WDHandle handle, WDInternalId internalId) {
            super("arraybuffer", handle, internalId);
        }
    }

    class NodeListRemoteValue extends BaseRemoteValue {
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

    class HTMLCollectionRemoteValue extends BaseRemoteValue {
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

     class NodeRemoteValue extends BaseRemoteValue {
        private final WDSharedId sharedId;
        private final NodeProperties value;

        public NodeRemoteValue(WDHandle handle, WDInternalId internalId, WDSharedId sharedId, NodeProperties value) {
            super("node", handle, internalId);
            this.sharedId = sharedId;
            this.value = value;
        }

        public WDSharedId getSharedId() {
            return sharedId;
        }

        public NodeProperties getValue() {
            return value;
        }

         public WDRemoteReference.SharedReference getSharedIdReference() {
             return new WDRemoteReference.SharedReference(this.getSharedId(), this.getHandle());
         }

        @Override
        public String asString() {
            return value.getNodeValue() != null ? value.getNodeValue() : "";
        }
     }

    class WindowProxyRemoteValue extends BaseRemoteValue {
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

    class NodeProperties {
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

    class WindowProxyProperties {
        private final WDBrowsingContext browsingContextRequest;

        public WindowProxyProperties(WDBrowsingContext browsingContext) {
            this.browsingContextRequest = browsingContext;
        }

        public WDBrowsingContext getBrowsingContext() {
            return browsingContextRequest;
        }
    }
}
