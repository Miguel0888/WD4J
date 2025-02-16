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
public interface WDRemoteReference {
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
                    return context.deserialize(jsonObject, SharedReferenceWD.class);
                case "remote-object-reference":
                    return context.deserialize(jsonObject, WDRemoteObjectReference.class);
                default:
                    throw new JsonParseException("Unknown RemoteReference type: " + type);
            }
        }
    }

    class SharedReferenceWD implements WDRemoteReference {
        private final String sharedId;
        private final String handle; // Optional

        public SharedReferenceWD(String sharedId) {
            this(sharedId, null);
        }

        public SharedReferenceWD(String sharedId, String handle) {
            this.sharedId = sharedId;
            this.handle = handle;
        }

        @Override
        public String getType() {
            return "shared-reference";
        }

        public String getSharedId() {
            return sharedId;
        }

        public String getHandle() {
            return handle;
        }
    }

    class WDRemoteObjectReference implements WDRemoteReference {
        private final String handle;
        private final String sharedId; // Optional

        public WDRemoteObjectReference(String handle) {
            this(handle, null);
        }

        public WDRemoteObjectReference(String handle, String sharedId) {
            this.handle = handle;
            this.sharedId = sharedId;
        }

        @Override
        public String getType() {
            return "remote-object-reference";
        }

        public String getHandle() {
            return handle;
        }

        public String getSharedId() {
            return sharedId;
        }
    }
}
