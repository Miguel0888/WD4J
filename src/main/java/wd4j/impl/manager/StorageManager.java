package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.StorageRequest;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.webdriver.command.request.parameters.storage.CookieFilter;
import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.CommunicationManager;

public class StorageManager implements Module {

    private final CommunicationManager communicationManager;

    public StorageManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
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
    public String getCookies(BrowsingContext contextId) {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new StorageRequest.GetCookies(contextId));
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new StorageRequest.SetCookie(contextId, cookie));
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new StorageRequest.DeleteCookies(contextId, cookieFilter));
            System.out.println("Cookie deleted: " + name + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error deleting cookie: " + e.getMessage());
            throw e;
        }
    }
}