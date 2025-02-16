package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(EvaluateResult.EvaluateResultAdapter.class) // 🔥 Direkt hier den Adapter registrieren
public interface EvaluateResult {
    String getType();

    // 🔥 **INNERE KLASSE: Adapter für die automatische Deserialisierung**
    class EvaluateResultAdapter implements JsonDeserializer<EvaluateResult> {
        @Override
        public EvaluateResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // 🔥 Prüfen, ob das "type"-Feld existiert
            if (!jsonObject.has("type") || jsonObject.get("type").isJsonNull()) {
                throw new JsonParseException("Missing or null 'type' field in EvaluateResult JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "success":
                    return context.deserialize(jsonObject, EvaluateResultSuccess.class);
                case "exception":
                    return context.deserialize(jsonObject, EvaluateResultError.class);
                default:
                    throw new JsonParseException("Unknown EvaluateResult type: " + type);
            }
        }
    }

    class EvaluateResultSuccess implements EvaluateResult {
        private final String type = "success";
        private final RemoteValue result;
        private final Realm realm;

        public EvaluateResultSuccess(RemoteValue result, Realm realm) {
            this.result = result;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public RemoteValue getResult() {
            return result;
        }

        public Realm getRealm() {
            return realm;
        }
    }

    class EvaluateResultError implements EvaluateResult {
        private final String type = "exception";
        private final ExceptionDetails exceptionDetails;
        private final Realm realm;

        public EvaluateResultError(ExceptionDetails exceptionDetails, Realm realm) {
            this.exceptionDetails = exceptionDetails;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public ExceptionDetails getExceptionDetails() {
            return exceptionDetails;
        }

        public Realm getRealm() {
            return realm;
        }
    }

}