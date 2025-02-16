package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.CreateType;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.SetViewportParameters;
import wd4j.impl.webdriver.type.browsingContext.WDLocator;
import wd4j.impl.websocket.WebSocketManager;

public class WDBrowsingContextManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDBrowsingContextManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
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
        WDBrowsingContextRequest.Create createContextCommand = new WDBrowsingContextRequest.Create(CreateType.TAB);

        try {
            String response = webSocketManager.sendAndWaitForResponse(createContextCommand, String.class);

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
        webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.Navigate(url, contextId), String.class);
    }

    public void getTree() {
        // Send the command, don't wait for the response
        webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.GetTree(), String.class);
    }

    /**
     * Activates the given browsing context.
     *
     * @param contextId The ID of the context to activate.
     * @throws RuntimeException if the activation fails.
     */
    public void activate(String contextId) {
        try {
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.Activate(contextId), String.class);
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
            String response = webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.CaptureScreenshot(contextId), String.class);
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
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.Close(contextId), String.class);
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
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.HandleUserPrompt(contextId, accept, userText), String.class);
        } catch (RuntimeException e) {
            System.out.println("Error handling user prompt: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param contextId The ID of the context to search in.
     * @param WDLocator  The CSS selector to locate nodes or the like.
     * @return The response containing the located nodes.
     */
    public String locateNodes(String contextId, WDLocator WDLocator) {
        try {
            String response = webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.LocateNodes(contextId, WDLocator), String.class);
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
            String response = webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.Print(contextId), String.class);
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
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.Reload(contextId), String.class);
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
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.SetViewport(contextId, new SetViewportParameters.Viewport(width, height), null), String.class);
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
            webSocketManager.sendAndWaitForResponse(new WDBrowsingContextRequest.TraverseHistory(contextId, delta), String.class);
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