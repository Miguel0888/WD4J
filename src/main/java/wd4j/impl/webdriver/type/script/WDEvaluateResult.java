package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(WDEvaluateResult.EvaluateResultAdapter.class) // 🔥 Direkt hier den Adapter registrieren
public interface WDEvaluateResult {
    String getType();

    // 🔥 **INNERE KLASSE: Adapter für die automatische Deserialisierung**
    class EvaluateResultAdapter implements JsonDeserializer<WDEvaluateResult> {
        @Override
        public WDEvaluateResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // 🔥 Prüfen, ob das "type"-Feld existiert
            if (!jsonObject.has("type") || jsonObject.get("type").isJsonNull()) {
                throw new JsonParseException("Missing or null 'type' field in EvaluateResult JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "success":
                    return context.deserialize(jsonObject, WDEvaluateResultSuccess.class);
                case "exception":
                    return context.deserialize(jsonObject, WDEvaluateResultError.class);
                default:
                    throw new JsonParseException("Unknown EvaluateResult type: " + type);
            }
        }
    }

    class WDEvaluateResultSuccess implements WDEvaluateResult {
        private final String type = "success";
        private final WDRemoteValue result;
        private final WDRealm realm;

        public WDEvaluateResultSuccess(WDRemoteValue result, WDRealm realm) {
            this.result = result;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public WDRemoteValue getResult() {
            return result;
        }

        public WDRealm getRealm() {
            return realm;
        }
    }

    class WDEvaluateResultError implements WDEvaluateResult {
        private final String type = "exception";
        private final WDExceptionDetails exceptionDetails;
        private final WDRealm realm;

        public WDEvaluateResultError(WDExceptionDetails exceptionDetails, WDRealm realm) {
            this.exceptionDetails = exceptionDetails;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public WDExceptionDetails getExceptionDetails() {
            return exceptionDetails;
        }

        public WDRealm getRealm() {
            return realm;
        }
    }

}