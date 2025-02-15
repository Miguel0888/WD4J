package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.BrowserRequest;
import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.webdriver.type.browser.ClientWindowInfo;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browser.UserContextInfo;
import wd4j.impl.websocket.CommunicationManager;

import java.util.ArrayList;
import java.util.List;

public class BrowserManager implements Module {

    private final CommunicationManager communicationManager;

    public ClientWindow clientWindow;
    public ClientWindowInfo clientWindowInfo;
    public UserContext userContext;
    public UserContextInfo userContextInfo;

    public BrowserManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Closes the browser.
     *
     * @throws RuntimeException if the close operation fails.
     */
    public void closeBrowser() {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.Close());
            System.out.println("Browser closed successfully.");
        } catch (RuntimeException e) {
            System.out.println("Error closing browser: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a new user context in the browser.
     *
     * @return The ID of the created user context.
     * @throws RuntimeException if the creation fails.
     */
    public String createUserContext() {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.CreateUserContext());
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            String contextId = jsonResponse.getAsJsonObject("result").get("context").getAsString();
            System.out.println("User context created: " + contextId);
            return contextId;
        } catch (RuntimeException e) {
            System.out.println("Error creating user context: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Retrieves the client windows of the browser.
     *
     * @return A list of client window IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getClientWindows() {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.GetClientWindows());
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
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.GetUserContexts());
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.RemoveUserContext(contextId));
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowserRequest.SetClientWindowState(clientWindowId, state));
            System.out.println("Client window state set: " + clientWindowId + " -> " + state);
        } catch (RuntimeException e) {
            System.out.println("Error setting client window state: " + e.getMessage());
            throw e;
        }
    }
}

