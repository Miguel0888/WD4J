package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.CreateType;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.SetViewportParameters;
import wd4j.impl.webdriver.type.browsingContext.Locator;
import wd4j.impl.websocket.CommunicationManager;

public class BrowsingContextManager implements Module {

    private final CommunicationManager communicationManager;

    public BrowsingContextManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new browsing context.
     *
     * @return The ID of the new context.
     */
    // Required for Firefox ESR ?
    public String create() {
        BrowsingContextRequest.Create createContextCommand = new BrowsingContextRequest.Create(CreateType.TAB);

        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(createContextCommand);

            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");

            if (result != null && result.has("context")) {
                String contextId = result.get("context").getAsString();
                System.out.println("--- Neuer Context erstellt: " + contextId);
                return contextId;
            }
        } catch (RuntimeException e) {
            System.out.println("Error creating context: " + e.getMessage());
            throw e;
        }

        throw new IllegalStateException("Failed to create new context."); // ToDo: Maybe return null instead?
    }


    /**
     * Navigates to the given URL within this browsing context.
     *
     * @param url The target URL to navigate to.
     * @return The response of the navigation command.
     */
    public void navigate(String url, String contextId) {
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalStateException("Cannot navigate: contextId is null or empty!");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Cannot navigate: URL is null or empty!");
        }
        communicationManager.getWebSocket().send(new BrowsingContextRequest.Navigate(url, contextId));
    }

    public void getTree() {
        // Send the command, don't wait for the response
        communicationManager.getWebSocket().send(new BrowsingContextRequest.GetTree());
    }

    /**
     * Activates the given browsing context.
     *
     * @param contextId The ID of the context to activate.
     * @throws RuntimeException if the activation fails.
     */
    public void activate(String contextId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.Activate(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error activating context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Captures a screenshot of the given browsing context.
     *
     * @param contextId The ID of the context to capture a screenshot from.
     * @return The screenshot as a base64-encoded string.
     */
    public String captureScreenshot(String contextId) {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.CaptureScreenshot(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").get("data").getAsString();
        } catch (RuntimeException e) {
            System.out.println("Error capturing screenshot: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Closes the given browsing context.
     *
     * @param contextId The ID of the context to close.
     * @throws RuntimeException if the close operation fails.
     */
    public void close(String contextId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.Close(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error closing context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Handles a user prompt in the given browsing context.
     *
     * @param contextId The ID of the context where the prompt should be handled. (required)
     * @param accept    Whether to accept or dismiss the prompt. (optional, may be null)
     * @param userText  The text to provide to the prompt, or null if no input is needed.
     *
     * @throws RuntimeException if handling the prompt fails.
     */
    public void handleUserPrompt(String contextId, Boolean accept, String userText) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.HandleUserPrompt(contextId, accept, userText));
        } catch (RuntimeException e) {
            System.out.println("Error handling user prompt: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param contextId The ID of the context to search in.
     * @param locator  The CSS selector to locate nodes or the like.
     * @return The response containing the located nodes.
     */
    public String locateNodes(String contextId, Locator locator) {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.LocateNodes(contextId, locator));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").toString();
        } catch (RuntimeException e) {
            System.out.println("Error locating nodes: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Prints the current page in the given browsing context.
     *
     * @param contextId The ID of the context to print.
     * @return The print output as a base64-encoded string.
     * @throws RuntimeException if the print operation fails.
     */
    public String print(String contextId) {
        try {
            String response = communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.Print(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").get("data").getAsString();
        } catch (RuntimeException e) {
            System.out.println("Error printing context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Reloads the given browsing context.
     *
     * @param contextId The ID of the context to reload.
     * @throws RuntimeException if the reload operation fails.
     */
    public void reload(String contextId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.Reload(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error reloading context: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Sets the viewport size of the given browsing context.
     *
     * @param contextId The ID of the context to resize.
     * @param width     The new width of the viewport in pixels.
     * @param height    The new height of the viewport in pixels.
     * @throws RuntimeException if setting the viewport size fails.
     */
    public void setViewport(String contextId, char width, char height) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.SetViewport(contextId, new SetViewportParameters.Viewport(width, height), null));
        } catch (RuntimeException e) {
            System.out.println("Error setting viewport: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Traverses the browsing history in the given context by a specific delta.
     *
     * @param contextId The ID of the context to navigate.
     * @param delta     The number of steps to move in the history (e.g., -1 for back, 1 for forward).
     * @throws RuntimeException if traversing history fails.
     */
    public void traverseHistory(String contextId, int delta) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new BrowsingContextRequest.TraverseHistory(contextId, delta));
        } catch (RuntimeException e) {
            System.out.println("Error traversing history: " + e.getMessage());
            throw e;
        }
    }

//    public Response traverseHistory(String contextId, int delta) {
//        // ToDo: Hier fehlt wohl doch eine JSON-Objekt-Mapper!
//        return webSocketImpl.sendAndWaitForResponse(new BrowsingContext.TraverseHistory(contextId, delta));
//    }

}