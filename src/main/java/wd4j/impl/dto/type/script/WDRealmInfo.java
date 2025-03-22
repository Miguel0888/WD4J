package wd4j.impl.dto.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;

/**
 * The script.RealmInfo type represents the properties of a realm. See {@link WDRealm}
 */
@JsonAdapter(WDRealmInfo.RealmInfoAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDRealmInfo {
    String getType();
    String getRealm();
    String getOrigin();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class RealmInfoAdapter implements JsonDeserializer<WDRealmInfo>, JsonSerializer<WDRealmInfo> {
        @Override
        public WDRealmInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in RealmInfo JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "window":
                    return context.deserialize(jsonObject, WindowWDRealmInfo.class);
                case "dedicated-worker":
                    return context.deserialize(jsonObject, DedicatedWorkerWDRealmInfo.class);
                case "shared-worker":
                    return context.deserialize(jsonObject, SharedWorkerWDRealmInfo.class);
                case "service-worker":
                    return context.deserialize(jsonObject, ServiceWorkerWDRealmInfo.class);
                case "worker":
                    return context.deserialize(jsonObject, WorkerWDRealmInfo.class);
                case "paint-worklet":
                    return context.deserialize(jsonObject, PaintWorkletWDRealmInfo.class);
                case "audio-worklet":
                    return context.deserialize(jsonObject, AudioWorkletWDRealmInfo.class);
                case "worklet":
                    return context.deserialize(jsonObject, WorkletWDRealmInfo.class);
                default:
                    throw new JsonParseException("Unknown RealmInfo type: " + type);
            }
        }

        @Override
        public JsonElement serialize(WDRealmInfo src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    abstract class BaseWDRealmInfo implements WDRealmInfo {
        private final String realm;
        private final String origin;

        public BaseWDRealmInfo(String realm, String origin) {
            this.realm = realm;
            this.origin = origin;
        }

        @Override
        public String getRealm() {
            return realm;
        }

        @Override
        public String getOrigin() {
            return origin;
        }
    }

    class WindowWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "window";
        private final String context;
        private final String sandbox; // Optional

        public WindowWDRealmInfo(String realm, String origin, String context) {
            this(realm, origin, context, null);
        }

        public WindowWDRealmInfo(String realm, String origin, String context, String sandbox) {
            super(realm, origin);
            this.context = context;
            this.sandbox = sandbox;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getContext() {
            return context;
        }

        public String getSandbox() {
            return sandbox;
        }
    }

    class DedicatedWorkerWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "dedicated-worker";
        private final List<String> owners;

        public DedicatedWorkerWDRealmInfo(String realm, String origin, List<String> owners) {
            super(realm, origin);
            this.owners = owners;
        }

        @Override
        public String getType() {
            return type;
        }

        public List<String> getOwners() {
            return owners;
        }
    }

    class SharedWorkerWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "shared-worker";

        public SharedWorkerWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class ServiceWorkerWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "service-worker";

        public ServiceWorkerWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class WorkerWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "worker";

        public WorkerWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class PaintWorkletWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "paint-worklet";

        public PaintWorkletWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class AudioWorkletWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "audio-worklet";

        public AudioWorkletWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class WorkletWDRealmInfo extends BaseWDRealmInfo implements WDRealmInfo {
        private final String type = "worklet";

        public WorkletWDRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }
}