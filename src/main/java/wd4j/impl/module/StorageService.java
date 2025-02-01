package wd4j.impl.module;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Module;
import wd4j.impl.module.command.Storage;

public class StorageService implements Module {

    private final WebSocketConnection webSocketConnection;

    public StorageService(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves cookies for the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @return A JSON string containing the cookies.
     * @throws RuntimeException if the operation fails.
     */
    public String getCookies(String contextId) {
        try {
            String response = webSocketConnection.send(new Storage.GetCookies(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").toString();
        } catch (RuntimeException e) {
            System.out.println("Error retrieving cookies: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param name      The name of the cookie.
     * @param value     The value of the cookie.
     * @throws RuntimeException if the operation fails.
     */
    public void setCookie(String contextId, String name, String value) {
        try {
            webSocketConnection.send(new Storage.SetCookie(contextId, name, value));
            System.out.println("Cookie set: " + name + " = " + value + " in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error setting cookie: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param name      The name of the cookie to delete.
     * @throws RuntimeException if the operation fails.
     */
    public void deleteCookie(String contextId, String name) {
        try {
            webSocketConnection.send(new Storage.DeleteCookies(contextId, name));
            System.out.println("Cookie deleted: " + name + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error deleting cookie: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}