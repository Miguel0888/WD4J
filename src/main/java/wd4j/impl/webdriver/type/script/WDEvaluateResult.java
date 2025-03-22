package wd4j.impl.webdriver.type.script;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.command.response.WDScriptResult;

import java.lang.reflect.Type;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// EvaluateResult:
// Diese Klasse unterscheidet sich von den anderen, da sie scheinbar eine vollständige Command-Response darstellt.
// Sie implementiert nämlich ein zusätzliches Feld type, nicht aber das Feld id. Laut Dokumentation ist es die Klasse
// auch ein Abkömmling von (WD)ScriptResult, was wiederum ResultData ist. Damit sollte es in der CommandResponse im
// Feld result übermittelt werden. Somit gäbe es hier zweimal das Feld type mit dem Inhalt "success" oder einmal
// mit "success" und einmal mit "exception". Das ist ein wenig ungewöhnlich, aber durchaus möglich. In dem Fall wird
// keine Java Exception geworfen, sondern ein spezieller State "exception" zurückgegeben, um zu signalisieren, dass
// der Fehler nicht in der Anwendung, sondern im Skript aufgetreten ist.
//
// Außerdem gibt es einen Typ "exception", der sich vom üblichen "error"-State unterscheidet und wohl durchgereicht
// werden soll, ohne eine Java Exception zu werfen. Es ist eher also normaler Programmablauf zu verstehen, da Exceptions
// in Skripten durchaus vorkommen können.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * The script.EvaluateResult type indicates the return value of a command that executes script.
 * The script.EvaluateResultSuccess variant is used in cases where the script completes normally and the
 * script.EvaluateResultException variant is used in cases where the script completes with a thrown exception.
 */
@JsonAdapter(WDEvaluateResult.EvaluateResultAdapter.class) // 🔥 Direkt hier den Adapter registrieren
public interface WDEvaluateResult extends WDScriptResult {
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