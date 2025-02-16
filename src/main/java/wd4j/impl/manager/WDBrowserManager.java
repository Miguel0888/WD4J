package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.BrowserRequest;
import wd4j.impl.webdriver.command.response.WDEmptyResult;
import wd4j.impl.webdriver.type.browser.WDClientWindow;
import wd4j.impl.webdriver.type.browser.WDClientWindowInfo;
import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browser.WDUserContextInfo;
import wd4j.impl.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.List;

public class WDBrowserManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDClientWindow WDClientWindow;
    public WDClientWindowInfo WDClientWindowInfo;
    public WDUserContext WDUserContext;
    public WDUserContextInfo WDUserContextInfo;

    public WDBrowserManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Closes the browser.
     */
    public void closeBrowser() {
        webSocketManager.sendAndWaitForResponse(new BrowserRequest.Close(), WDEmptyResult.class);
        System.out.println("Browser closed successfully.");
    }

    /**
     * Creates a new user context in the browser.
     *
     * @return The created user context DTO.
     */
    public WDUserContextInfo createUserContext() {
        WDUserContextInfo result =
                webSocketManager.sendAndWaitForResponse(new BrowserRequest.CreateUserContext(), WDUserContextInfo.class);
        System.out.println("User context created: " + result.getUserContext().value());
        return result;
    }

    /**
     * Retrieves the client windows of the browser.
     *
     * @return A list of client window IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getClientWindows() {
        try {
            String response = webSocketManager.sendAndWaitForResponse(new BrowserRequest.GetClientWindows(), String.class);
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray windows = jsonResponse.getAsJsonObject("result").getAsJsonArray("windows");
            List<String> windowIds = new ArrayList<>();
            windows.forEach(window -> windowIds.add(window.getAsString()));
            System.out.println("Client windows retrieved: " + windowIds);
            return windowIds;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving client windows: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Retrieves the user contexts available in the browser.
     *
     * @return A list of user context IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getUserContexts() {
        try {
            String response = webSocketManager.sendAndWaitForResponse(new BrowserRequest.GetUserContexts(), String.class);
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray contexts = jsonResponse.getAsJsonObject("result").getAsJsonArray("contexts");
            List<String> contextIds = new ArrayList<>();
            contexts.forEach(context -> contextIds.add(context.getAsString()));
            System.out.println("User contexts retrieved: " + contextIds);
            return contextIds;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving user contexts: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Removes a user context from the browser.
     *
     * @param contextId The ID of the user context to remove.
     * @throws RuntimeException if the removal fails.
     */
    public void removeUserContext(String contextId) {
        try {
            webSocketManager.sendAndWaitForResponse(new BrowserRequest.RemoveUserContext(contextId), String.class);
            System.out.println("User context removed: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error removing user context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets the state of a client window.
     *
     * @param clientWindowId The ID of the client window.
     * @param state          The state to set (e.g., "minimized", "maximized").
     * @throws RuntimeException if setting the state fails.
     */
    public void setClientWindowState(String clientWindowId, String state) {
        try {
            webSocketManager.sendAndWaitForResponse(new BrowserRequest.SetClientWindowState(clientWindowId, state), String.class);
            System.out.println("Client window state set: " + clientWindowId + " -> " + state);
        } catch (RuntimeException e) {
            System.out.println("Error setting client window state: " + e.getMessage());
            throw e;
        }
    }
}

