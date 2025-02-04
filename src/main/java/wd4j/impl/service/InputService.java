package wd4j.impl.service;

import wd4j.impl.module.generic.Module;
import wd4j.impl.module.command.Input;
import wd4j.impl.playwright.WebSocketImpl;

import java.util.List;

public class InputService implements Module {

    private final WebSocketImpl webSocketImpl;

    public InputService(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
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
    public void performActions(String contextId, List<Input.PerformActions.Action> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("Actions list must not be null or empty.");
        }

        try {
            webSocketImpl.sendAndWaitForResponse(new Input.PerformActions(contextId, actions));
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
            webSocketImpl.sendAndWaitForResponse(new Input.ReleaseActions(contextId));
            System.out.println("Released actions in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error releasing actions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets files to the given input element.
     *
     * @param elementId The ID of the input element.
     * @param filePaths A list of file paths to set.
     * @throws RuntimeException if the operation fails.
     */
    public void setFiles(String elementId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            throw new IllegalArgumentException("File paths list must not be null or empty.");
        }

        try {
            webSocketImpl.sendAndWaitForResponse(new Input.SetFiles(elementId, filePaths));
            System.out.println("Files set for element: " + elementId);
        } catch (RuntimeException e) {
            System.out.println("Error setting files: " + e.getMessage());
            throw e;
        }
    }
}