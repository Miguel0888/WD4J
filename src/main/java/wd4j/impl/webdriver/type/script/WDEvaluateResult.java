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
        private final WDRealm WDRealm;

        public WDEvaluateResultSuccess(WDRemoteValue result, WDRealm WDRealm) {
            this.result = result;
            this.WDRealm = WDRealm;
        }

        @Override
        public String getType() {
            return type;
        }

        public WDRemoteValue getResult() {
            return result;
        }

        public WDRealm getRealm() {
            return WDRealm;
        }
    }

    class WDEvaluateResultError implements WDEvaluateResult {
        private final String type = "exception";
        private final WDExceptionDetails WDExceptionDetails;
        private final WDRealm WDRealm;

        public WDEvaluateResultError(WDExceptionDetails WDExceptionDetails, WDRealm WDRealm) {
            this.WDExceptionDetails = WDExceptionDetails;
            this.WDRealm = WDRealm;
        }

        @Override
        public String getType() {
            return type;
        }

        public WDExceptionDetails getExceptionDetails() {
            return WDExceptionDetails;
        }

        public WDRealm getRealm() {
            return WDRealm;
        }
    }

}