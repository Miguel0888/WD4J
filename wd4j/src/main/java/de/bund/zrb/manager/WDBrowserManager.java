package de.bund.zrb.manager;

import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.command.request.WDBrowserRequest;
import de.bund.zrb.command.response.WDBrowserResult;
import de.bund.zrb.command.response.WDEmptyResult;
import de.bund.zrb.type.browser.WDClientWindowInfo;
import de.bund.zrb.type.browser.WDUserContextInfo;
import de.bund.zrb.api.WebSocketManager;

public class WDBrowserManager implements WDModule {

    private final WebSocketManager webSocketManager;

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
        webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.Close(), WDEmptyResult.class);
        System.out.println("Browser closed successfully.");
    }

    /**
     * Creates a new user context in the browser aka. a new page in DevTools terminology.
     *
     * @return The created user context DTO.
     */
    public WDBrowserResult.CreateUserContextResult createUserContext() {
        WDBrowserResult.CreateUserContextResult result =
                webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.CreateUserContext(), WDUserContextInfo.class);
        System.out.println("User context created: " + result.getUserContext().value());
        return result;
    }

    /**
     * Retrieves the client windows of the browser.
     *
     * @return A list of client window IDs.
     * @throws RuntimeException if the operation fails.
     */
    public WDBrowserResult.GetClientWindowsResult getClientWindows() {
        WDBrowserResult.GetClientWindowsResult result =
                webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.GetClientWindows(), WDBrowserResult.GetClientWindowsResult.class);

        System.out.println("Client windows retrieved: " + result.getClientWindows());
        return result;
    }


    /**
     * Retrieves the user contexts available in the browser.
     *
     * @return A list of user context IDs.
     * @throws RuntimeException if the operation fails.
     */
    public WDBrowserResult.GetUserContextsResult getUserContexts() {
        WDBrowserResult.GetUserContextsResult result =
                webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.GetUserContexts(), WDBrowserResult.GetUserContextsResult.class);

        System.out.println("User contexts retrieved: " + result.getUserContexts());
        return result;
    }


    /**
     * Removes a user context from the browser.
     *
     * @param contextId The ID of the user context to remove.
     * @throws RuntimeException if the removal fails.
     */
    public void removeUserContext(String contextId) {
        webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.RemoveUserContext(contextId), WDEmptyResult.class);
        System.out.println("User context removed: " + contextId);
    }

    /**
     * Sets the state of a client window.
     *
     * @param clientWindowId The ID of the client window.
     * @param state          The state to set (e.g., "minimized", "maximized").
     * @throws RuntimeException if setting the state fails.
     */
    public WDClientWindowInfo setClientWindowState(String clientWindowId, String state) {
        WDClientWindowInfo result = webSocketManager.sendAndWaitForResponse(new WDBrowserRequest.SetClientWindowState(clientWindowId, state), WDClientWindowInfo.class);
        System.out.println("Client window state set: " + result.getClientWindow().value());
        return result;
    }
}

