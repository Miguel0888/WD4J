package wd4j.impl.webdriver.type.script;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;

import java.lang.reflect.Type;

/**
 * The script.Target type represents a value that is either a script.Realm or a browsingContext.BrowsingContext.
 *
 * This is useful in cases where a navigable identifier can stand in for the realm associated with the navigable’s
 * active document.
 */
@JsonAdapter(WDTarget.WDTargetAdapter.class) // 🔥 Automatische JSON-Deserialisierung
public abstract class WDTarget {

    // 🔥 **JSON Adapter für automatische Deserialisierung**
    public static class WDTargetAdapter implements JsonDeserializer<WDTarget> {
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
    }

    public static class RealmTarget extends WDTarget {
        private final WDRealm realm;

        public RealmTarget(WDRealm realm) {
            this.realm = realm;
        }

        public WDRealm getRealm() {
            return realm;
        }
    }

    public static class ContextTarget extends WDTarget {
        private final WDBrowsingContext context;
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


