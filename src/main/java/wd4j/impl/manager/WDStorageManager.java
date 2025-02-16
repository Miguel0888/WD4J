package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.StorageRequest;
import wd4j.impl.webdriver.command.request.parameters.storage.CookieFilter;
import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WebSocketManager;

public class WDStorageManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDStorageManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves cookies for the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @return A JSON string containing the cookies.
     * @throws RuntimeException if the operation fails.
     */
    public String getCookies(WDBrowsingContext contextId) {
        try {
            String response = webSocketManager.sendAndWaitForResponse(new StorageRequest.GetCookies(contextId), String.class);
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
     * @param cookie      The name and value of the cookie.
     * @throws RuntimeException if the operation fails.
     */
    public void setCookie(String contextId, SetCookieParameters.PartialCookie cookie) {
        try {
            webSocketManager.sendAndWaitForResponse(new StorageRequest.SetCookie(contextId, cookie), String.class);
            System.out.println("Cookie set: " + cookie.getName() + " = " + cookie.getValue() + " in context: " + contextId);
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
            CookieFilter cookieFilter = new CookieFilter(name, null, null, null, null, null, null, null, null);
            webSocketManager.sendAndWaitForResponse(new StorageRequest.DeleteCookies(contextId, cookieFilter), String.class);
            System.out.println("Cookie deleted: " + name + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error deleting cookie: " + e.getMessage());
            throw e;
        }
    }
}