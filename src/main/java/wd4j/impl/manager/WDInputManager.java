package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.InputRequest;
import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;
import wd4j.impl.webdriver.type.script.WDRemoteReference;
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
    public void performActions(String contextId, List<PerformActionsParameters.SourceActions> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("Actions list must not be null or empty.");
        }

        try {
            webSocketManager.sendAndWaitForResponse(new InputRequest.PerformActions(contextId, actions), String.class);
            System.out.println("Performed actions in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error performing actions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Releases all input actions for the given browsing context.
     *
     * @param contextId The ID of the context where the actions are released.
     * @throws RuntimeException if the release operation fails.
     */
    public void releaseActions(String contextId) {
        try {
            webSocketManager.sendAndWaitForResponse(new InputRequest.ReleaseActions(contextId), String.class);
            System.out.println("Released actions in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error releasing actions: " + e.getMessage());
            throw e;
        }
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
    public void setFiles(String contextId, WDRemoteReference.SharedReferenceWD sharedReference, List<String> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("File paths list must not be null or empty.");
        }

        try {
            webSocketManager.sendAndWaitForResponse(new InputRequest.SetFiles(contextId, sharedReference, files), String.class);
            System.out.println("Files set for element: " + sharedReference);
        } catch (RuntimeException e) {
            System.out.println("Error setting files: " + e.getMessage());
            throw e;
        }
    }
}