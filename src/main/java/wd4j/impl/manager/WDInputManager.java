package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.dto.command.request.WDInputRequest;
import wd4j.impl.dto.command.request.parameters.input.sourceActions.SourceActions;
import wd4j.impl.dto.command.response.WDEmptyResult;
import wd4j.impl.dto.type.script.WDRemoteReference;
import wd4j.impl.websocket.WebSocketManager;

import java.util.List;

public class WDInputManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDInputManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

     /**
     * Performs a sequence of input actions in the given browsing context.
     *
     * @param contextId The ID of the context where the actions are performed.
     * @param actions   A list of actions to perform.
     * @throws RuntimeException if the action execution fails.
     */
    /**
     * Performs a sequence of input actions in the given browsing context.
     *
     * @param contextId The ID of the context where the actions are performed.
     * @param actions   A list of actions to perform.
     * @throws RuntimeException if the action execution fails.
     */
    public void performActions(String contextId, List<SourceActions> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("Actions list must not be null or empty.");
        }

        webSocketManager.sendAndWaitForResponse(new WDInputRequest.PerformActions(contextId, actions), WDEmptyResult.class);
        System.out.println("Performed actions in context: " + contextId);
    }

    /**
     * Releases all input actions for the given browsing context.
     *
     * @param contextId The ID of the context where the actions are released.
     * @throws RuntimeException if the release operation fails.
     */
    public void releaseActions(String contextId) {
        webSocketManager.sendAndWaitForResponse(new WDInputRequest.ReleaseActions(contextId), WDEmptyResult.class);
        System.out.println("Released actions in context: " + contextId);
    }

    /**
     * Sets files to the given input element.
     *
     * @param contextId       The ID of the context where the element is located.
     * @param sharedReference The shared reference of the element.
     * @param files           A list of file paths to set.
     *
     * @throws RuntimeException if the operation fails.
     */
    public void setFiles(String contextId, WDRemoteReference.SharedReference sharedReference, List<String> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("File paths list must not be null or empty.");
        }

        webSocketManager.sendAndWaitForResponse(new WDInputRequest.SetFiles(contextId, sharedReference, files), WDEmptyResult.class);
        System.out.println("Files set for element: " + sharedReference);
    }
}