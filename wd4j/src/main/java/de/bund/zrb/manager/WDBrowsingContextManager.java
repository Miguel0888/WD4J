package de.bund.zrb.manager;

import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.command.request.WDBrowsingContextRequest;
import de.bund.zrb.command.request.parameters.browsingContext.CaptureScreenshotParameters;
import de.bund.zrb.command.request.parameters.browsingContext.CreateType;
import de.bund.zrb.command.request.parameters.browsingContext.SetViewportParameters;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDEmptyResult;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.type.script.WDRemoteReference;
import de.bund.zrb.type.script.WDSerializationOptions;
import de.bund.zrb.api.WDWebSocketManager;

import java.util.List;

public class WDBrowsingContextManager implements WDModule {

    private final WDWebSocketManager WDWebSocketManager;

    public WDBrowsingContextManager(WDWebSocketManager WDWebSocketManager) {
        this.WDWebSocketManager = WDWebSocketManager;
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
    public WDBrowsingContextResult.CreateResult create() {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Create(CreateType.TAB),
                WDBrowsingContextResult.CreateResult.class
        );
    }

    public WDBrowsingContextResult.CreateResult create(CreateType type, WDBrowsingContext referenceContext, Boolean background, WDUserContext userContext) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Create(type, referenceContext, background, userContext),
                WDBrowsingContextResult.CreateResult.class
        );
    }

    /**
     * Navigates to the given URL within this browsing context.
     *
     * @param url The target URL to navigate to.
     * @return The response of the navigation command.
     */
    public WDBrowsingContextResult.NavigateResult navigate(String url, String contextId) {
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalStateException("Cannot navigate: contextId is null or empty!");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Cannot navigate: URL is null or empty!");
        }

        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Navigate(url, contextId),
                WDBrowsingContextResult.NavigateResult.class
        );
    }


    public WDBrowsingContextResult.GetTreeResult getTree() {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.GetTree(),
                WDBrowsingContextResult.GetTreeResult.class
        );
    }

    public WDBrowsingContextResult.GetTreeResult getTree(String browsingContextId) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.GetTree(new WDBrowsingContext(browsingContextId)),
                WDBrowsingContextResult.GetTreeResult.class
        );
    }

    public WDBrowsingContextResult.GetTreeResult getTree(WDBrowsingContext context) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.GetTree(context),
                WDBrowsingContextResult.GetTreeResult.class
        );
    }

    public WDBrowsingContextResult.GetTreeResult getTree(WDBrowsingContext context, Long depth) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.GetTree(context, depth),
                WDBrowsingContextResult.GetTreeResult.class
        );
    }

    /**
     * Activates the given browsing context.
     *
     * @param contextId The ID of the context to activate.
     * @throws RuntimeException if the activation fails.
     */
    public void activate(String contextId) {
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Activate(contextId),
                WDEmptyResult.class
        );
    }


    /**
     * Captures a screenshot of the given browsing context.
     *
     * @param contextId The ID of the context to capture a screenshot from.
     * @return The screenshot as a base64-encoded string.
     */
    public WDBrowsingContextResult.CaptureScreenshotResult captureScreenshot(String contextId) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.CaptureScreenshot(contextId),
                WDBrowsingContextResult.CaptureScreenshotResult.class
        );
    }

    /**
     * Captures a screenshot of the given browsing context.
     *
     * @param contextId The ID of the context to capture a screenshot from.
     * @return The screenshot as a base64-encoded string.
     */
    public WDBrowsingContextResult.CaptureScreenshotResult captureScreenshot(WDBrowsingContext context, CaptureScreenshotParameters.Origin origin, CaptureScreenshotParameters.ImageFormat format, CaptureScreenshotParameters.ClipRectangle clip) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.CaptureScreenshot(context, origin, format, clip),
                WDBrowsingContextResult.CaptureScreenshotResult.class
        );
    }

    /**
     * Closes the given browsing context.
     *
     * @param contextId The ID of the context to close.
     * @throws RuntimeException if the close operation fails.
     */
    public void close(String contextId) {
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Close(contextId, null),
                WDEmptyResult.class
        );
    }

    /**
     * Closes the given browsing context.
     *
     * @param contextId The ID of the context to close.
     * @param prompt Whether to prompt the user before closing the context.
     * @throws RuntimeException if the close operation fails.
     */
    public void close(String contextId, Boolean prompt) {
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Close(contextId, prompt),
                WDEmptyResult.class
        );
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
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.HandleUserPrompt(contextId, accept, userText),
                WDEmptyResult.class
        );
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param contextId The ID of the context to search in.
     * @param locator  The CSS selector to locate nodes or the like.
     * @return The response containing the located nodes.
     */
    public WDBrowsingContextResult.LocateNodesResult locateNodes(String contextId, WDLocator locator) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.LocateNodes(contextId, locator),
                WDBrowsingContextResult.LocateNodesResult.class
        );
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param contextId The ID of the context to search in.
     * @param locator  The CSS selector to locate nodes or the like.
     * @return The response containing the located nodes.
     */
    public WDBrowsingContextResult.LocateNodesResult locateNodes(String contextId, WDLocator locator, Integer maxNodeCount) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.LocateNodes(contextId, locator, maxNodeCount),
                WDBrowsingContextResult.LocateNodesResult.class
        );
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param context The context to search in.
     * @param locator  The CSS selector to locate nodes or the like.
     *                                @return The response containing the located nodes.
     * @return The response containing the located nodes.
     */
    public WDBrowsingContextResult.LocateNodesResult locateNodes(WDBrowsingContext context, WDLocator locator, Integer maxNodeCount) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.LocateNodes(context, locator, maxNodeCount),
                WDBrowsingContextResult.LocateNodesResult.class
        );
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param context The context to search in.
     * @param locator  The CSS selector to locate nodes or the like.
     *                                @return The response containing the located nodes.
     * @return The response containing the located nodes.
     */
    public WDBrowsingContextResult.LocateNodesResult locateNodes(WDBrowsingContext context, WDLocator locator, Integer maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReference> startNodes) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.LocateNodes(context, locator, maxNodeCount, WDSerializationOptions, startNodes),
                WDBrowsingContextResult.LocateNodesResult.class
        );
    }



        /**
         * Prints the current page in the given browsing context.
         *
         * @param contextId The ID of the context to print.
         * @return The print output as a base64-encoded string.
         * @throws RuntimeException if the print operation fails.
         */
    public WDBrowsingContextResult.PrintResult print(String contextId) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Print(contextId),
                WDBrowsingContextResult.PrintResult.class
        );
    }

    /**
     * Reloads the given browsing context.
     *
     * @param contextId The ID of the context to reload.
     * @throws RuntimeException if the reload operation fails.
     */
    public void reload(String contextId) {
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.Reload(contextId),
                WDEmptyResult.class
        );
    }

    /**
     * Sets the viewport size of the given browsing context.
     *
     * @param contextId The ID of the context to resize.
     * @param width     The new width of the viewport in pixels.
     * @param height    The new height of the viewport in pixels.
     * @throws RuntimeException if setting the viewport size fails.
     */
    public void setViewport(String contextId, int width, int height) {
        WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.SetViewport(
                        contextId,
                        new SetViewportParameters.Viewport(width, height),
                        null
                ),
                WDEmptyResult.class
        );
    }

    /**
     * Traverses the browsing history in the given context by a specific delta.
     *
     * @param contextId The ID of the context to navigate.
     * @param delta     The number of steps to move in the history (e.g., -1 for back, 1 for forward).
     * @throws RuntimeException if traversing history fails.
     */
    public WDBrowsingContextResult.TraverseHistoryResult traverseHistory(String contextId, int delta) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDBrowsingContextRequest.TraverseHistory(contextId, delta),
                WDBrowsingContextResult.TraverseHistoryResult.class
        );
    }
}