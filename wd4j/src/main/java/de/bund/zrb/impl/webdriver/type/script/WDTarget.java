package de.bund.zrb.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;

import java.lang.reflect.Type;

/**
 * The script.Target type represents a value that is either a script.Realm or a browsingContext.BrowsingContext.
 */
@JsonAdapter(WDTarget.WDTargetAdapter.class) // 🔥 Automatische JSON-Serialisierung
public interface WDTarget {

    // 🔥 **JSON Adapter für automatische Serialisierung/Deserialisierung**
    class WDTargetAdapter implements JsonDeserializer<WDTarget>, JsonSerializer<WDTarget> {
        @Override
        public WDTarget deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (jsonObject.has("realm")) {
                return context.deserialize(jsonObject, RealmTarget.class);
            } else if (jsonObject.has("context")) {
                return context.deserialize(jsonObject, ContextTarget.class);
            } else {
                throw new JsonParseException("Unknown WDTarget type: missing 'realm' or 'context' field");
            }
        }

        @Override
        public JsonElement serialize(WDTarget src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    /**
     * `script.RealmTarget` = `{ realm: script.Realm }`
     */
    class RealmTarget implements WDTarget {
        private final WDRealm realm;

        public RealmTarget(WDRealm realm) {
            this.realm = realm;
        }

        public WDRealm getRealm() {
            return realm;
        }
    }

    /**
     * `script.ContextTarget` = `{ context: browsingContext.BrowsingContext, ?sandbox: text }`
     */
    class ContextTarget implements WDTarget {
        private final WDBrowsingContext context; // 🔥 Jetzt korrekt als WDBrowsingContext gespeichert!
        private final String sandbox; // Optional

        public ContextTarget(WDBrowsingContext context) {
            this(context, null);
        }

        public ContextTarget(WDBrowsingContext context, String sandbox) {
            this.context = context;
            this.sandbox = sandbox;
        }

        public WDBrowsingContext getContext() {
            return context;
        }

        public String getSandbox() {
            return sandbox;
        }
    }
}
