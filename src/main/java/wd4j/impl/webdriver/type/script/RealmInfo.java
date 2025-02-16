package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;

/**
 * The script.RealmInfo type represents the properties of a realm. See {@link Realm}
 */
@JsonAdapter(RealmInfo.RealmInfoAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface RealmInfo {
    String getType();
    String getRealm();
    String getOrigin();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class RealmInfoAdapter implements JsonDeserializer<RealmInfo> {
        @Override
        public RealmInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in RealmInfo JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "window":
                    return context.deserialize(jsonObject, WindowRealmInfo.class);
                case "dedicated-worker":
                    return context.deserialize(jsonObject, DedicatedWorkerRealmInfo.class);
                case "shared-worker":
                    return context.deserialize(jsonObject, SharedWorkerRealmInfo.class);
                case "service-worker":
                    return context.deserialize(jsonObject, ServiceWorkerRealmInfo.class);
                case "worker":
                    return context.deserialize(jsonObject, WorkerRealmInfo.class);
                case "paint-worklet":
                    return context.deserialize(jsonObject, PaintWorkletRealmInfo.class);
                case "audio-worklet":
                    return context.deserialize(jsonObject, AudioWorkletRealmInfo.class);
                case "worklet":
                    return context.deserialize(jsonObject, WorkletRealmInfo.class);
                default:
                    throw new JsonParseException("Unknown RealmInfo type: " + type);
            }
        }
    }

    abstract class BaseRealmInfo implements RealmInfo {
        private final String realm;
        private final String origin;

        public BaseRealmInfo(String realm, String origin) {
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

    class WindowRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "window";
        private final String context;
        private final String sandbox; // Optional

        public WindowRealmInfo(String realm, String origin, String context) {
            this(realm, origin, context, null);
        }

        public WindowRealmInfo(String realm, String origin, String context, String sandbox) {
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

    class DedicatedWorkerRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "dedicated-worker";
        private final List<String> owners;

        public DedicatedWorkerRealmInfo(String realm, String origin, List<String> owners) {
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

    class SharedWorkerRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "shared-worker";

        public SharedWorkerRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class ServiceWorkerRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "service-worker";

        public ServiceWorkerRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class WorkerRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "worker";

        public WorkerRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class PaintWorkletRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "paint-worklet";

        public PaintWorkletRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class AudioWorkletRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "audio-worklet";

        public AudioWorkletRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class WorkletRealmInfo extends BaseRealmInfo implements RealmInfo {
        private final String type = "worklet";

        public WorkletRealmInfo(String realm, String origin) {
            super(realm, origin);
        }

        @Override
        public String getType() {
            return type;
        }
    }
}