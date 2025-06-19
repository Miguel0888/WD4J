package de.bund.zrb.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * The script.RemoteReference type is either a script.RemoteObjectReference representing a remote reference to an
 * existing ECMAScript object in handle object map in the given Realm, or is a script.SharedReference representing a
 * reference to a node.
 */
@JsonAdapter(WDRemoteReference.RemoteReferenceAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDRemoteReference<T> extends WDLocalValue {
    String getType();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class RemoteReferenceAdapter<T> implements JsonDeserializer<WDRemoteReference<T>> {
        @Override
        public WDRemoteReference<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in RemoteReference JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "shared-reference":
                    return context.deserialize(jsonObject, SharedReference.class);
                case "remote-object-reference":
                    return context.deserialize(jsonObject, RemoteObjectReference.class);
                default:
                    throw new JsonParseException("Unknown RemoteReference type: " + type);
            }
        }
    }

    class SharedReference implements WDRemoteReference<SharedReference> {
        private final WDSharedId sharedId;
        private final WDHandle handle; // Optional

        public SharedReference(WDSharedId sharedId) {
            this(sharedId, null);
        }

        public SharedReference(WDSharedId sharedId, WDHandle handle) {
            this.sharedId = sharedId;
            this.handle = handle;
        }

        @Override
        public String getType() {
            return "shared-reference";
        }

        public WDSharedId getSharedId() {
            return sharedId;
        }

        public WDHandle getHandle() {
            return handle;
        }
    }

    class RemoteObjectReference implements WDRemoteReference<RemoteObjectReference> {
        private final WDHandle handle;
        private final WDSharedId sharedId; // Optional

        public RemoteObjectReference(WDHandle handle) {
            this(handle, null);
        }

        public RemoteObjectReference(WDSharedId sharedId) {
            this(null, sharedId);
        }

        public RemoteObjectReference(WDHandle handle, WDSharedId sharedId) {
            this.handle = handle; // CAUTION: Only valid in the context of the given Realm (user needs to ensure this)
            this.sharedId = sharedId; // Always valid
        }

        @Override
        public String getType() {
            return "remote-object-reference";
        }

        public WDHandle getHandle() {
            return handle;
        }

        public WDSharedId getSharedId() {
            return sharedId;
        }
    }
}
