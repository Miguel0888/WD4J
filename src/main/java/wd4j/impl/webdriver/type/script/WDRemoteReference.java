package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * The script.RemoteReference type is either a script.RemoteObjectReference representing a remote reference to an
 * existing ECMAScript object in handle object map in the given Realm, or is a script.SharedReference representing a
 * reference to a node.
 */
@JsonAdapter(WDRemoteReference.RemoteReferenceAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDRemoteReference extends WDLocalValue {
    String getType();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class RemoteReferenceAdapter implements JsonDeserializer<WDRemoteReference> {
        @Override
        public WDRemoteReference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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

    class SharedReference implements WDRemoteReference {
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

    class RemoteObjectReference implements WDRemoteReference {
        private final WDHandle handle;
        private final WDSharedId sharedId; // Optional

        public RemoteObjectReference(WDHandle handle) {
            this(handle, null);
        }

        public RemoteObjectReference(WDHandle handle, WDSharedId sharedId) {
            this.handle = handle;
            this.sharedId = sharedId;
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
