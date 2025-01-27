package wd4j.impl.generic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Interface for WebDriver BiDi commands.
 *
 *
 */
public interface Command {

    /** The WebDriver BiDi command name */
    String getName();

    /** Lets the WebClientConnection set the WebDriver BiDi command ID */
    void setId(Integer id);

    /** Gets WebDriver BiDi command ID, which is used to identify the command response */
    Integer getId();

    /**
     * Converts the command to a JsonObject.
     *
     * @return JsonObject representation of the command
     */
    default JsonObject toJson() {
        Gson gson = new Gson();
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
        default JsonObject toJson() {
            Gson gson = new Gson();
            return gson.toJsonTree(this).getAsJsonObject();
        }
    }
}