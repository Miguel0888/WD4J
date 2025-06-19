package de.bund.zrb.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;

/**
 * The script.RealmInfo type represents the properties of a realm. See {@link WDRealm}
 */
@JsonAdapter(WDRealmInfo.RealmInfoAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDRealmInfo {
    WDRealmType getType(); // ToDo: Use Enum
    WDRealm getRealm();
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

        @Override
        public JsonElement serialize(WDRealmInfo src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    abstract class BaseRealmInfo implements WDRealmInfo {
        private final WDRealm realm;
        private final String origin;

        public BaseRealmInfo(WDRealm realm, String origin) {
            this.realm = realm;
            this.origin = origin;
        }

        @Override
        public WDRealm getRealm() {
            return realm;
        }

        @Override
        public String getOrigin() {
            return origin;
        }
    }

    class WindowRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.WINDOW;
        private final String context;
        private final String sandbox; // Optional

        public WindowRealmInfo(WDRealm realm, String origin, String context) {
            this(realm, origin, context, null);
        }

        public WindowRealmInfo(WDRealm realm, String origin, String context, String sandbox) {
            super(realm, origin);
            this.context = context;
            this.sandbox = sandbox;
        }

        @Override
        public WDRealmType getType() {
            return type;
        }

        public String getContext() {
            return context;
        }

        public String getSandbox() {
            return sandbox;
        }
    }

    class DedicatedWorkerRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.DEDICATED_WORKER;
        private final List<String> owners;

        public DedicatedWorkerRealmInfo(WDRealm realm, String origin, List<String> owners) {
            super(realm, origin);
            this.owners = owners;
        }

        @Override
        public WDRealmType getType() {
            return type;
        }

        public List<String> getOwners() {
            return owners;
        }
    }

    class SharedWorkerRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.SHARED_WORKER;

        public SharedWorkerRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }

    class ServiceWorkerRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.SERVICE_WORKER;

        public ServiceWorkerRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }

    class WorkerRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.WORKER;

        public WorkerRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }

    class PaintWorkletRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.PAINT_WORKLET;

        public PaintWorkletRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }

    class AudioWorkletRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.AUDIO_WORKLET;

        public AudioWorkletRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }

    class WorkletRealmInfo extends BaseRealmInfo implements WDRealmInfo {
        private final WDRealmType type = WDRealmType.WORKLET;

        public WorkletRealmInfo(WDRealm realm, String origin) {
            super(realm, origin);
        }

        @Override
        public WDRealmType getType() {
            return type;
        }
    }
}