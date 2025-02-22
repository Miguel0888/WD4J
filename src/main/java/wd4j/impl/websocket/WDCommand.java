package wd4j.impl.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.webdriver.mapping.GsonMapperFactory;

/**
 * Interface for WebDriver BiDi commands.
 *
 *
 */
public interface WDCommand extends WDType {

    /** The WebDriver BiDi command name */
    String getName();

    /** Gets WebDriver BiDi command ID, which is used to identify the command response */
    Integer getId();

    /**
     * Converts the command to a JsonObject.
     *
     * @return JsonObject representation of the command
     */
    default JsonObject toJson() {
        Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik
        return gson.toJsonTree(this).getAsJsonObject();
    }

    /**
     * Interface for command parameters.
     */
    interface Params {
        /**
         * Converts the command to a JsonObject.
         *
         * @return JsonObject representation of the command
         */
        // ToDo: Maybe removed later? Currently some Commands use JSON instead of the Param-DTOs
        default JsonObject toJson() {
            Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale GSON-Instanz mit allen Einstellungen
            return gson.toJsonTree(this).getAsJsonObject();
        }
    }
}