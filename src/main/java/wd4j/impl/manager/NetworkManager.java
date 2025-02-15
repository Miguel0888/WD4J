package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.NetworkRequest;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.webdriver.command.request.parameters.network.AddInterceptParameters;
import wd4j.impl.webdriver.command.request.parameters.network.SetCacheBehaviorParameters;
import wd4j.impl.webdriver.type.network.AuthCredentials;
import wd4j.impl.websocket.CommunicationManager;

import java.util.List;

public class NetworkManager implements Module {

    private final CommunicationManager communicationManager;

    public NetworkManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds an intercept for network requests matching the given URL pattern.
     *
     * @param phases The phases to intercept.
     *
     * @throws RuntimeException if the operation fails.
     */
    public void addIntercept(List<AddInterceptParameters.InterceptPhase> phases) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.AddIntercept(phases));
            System.out.println("Intercept added for intercept phases: " + phases);
        } catch (RuntimeException e) {
            System.out.println("Error adding intercept: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network request with the given request ID.
     *
     * @param requestId The ID of the request to continue.
     * @throws RuntimeException if the operation fails.
     */
    public void continueRequest(String requestId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.ContinueRequest(requestId));
            System.out.println("Request continued: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing request: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network response with the given request ID.
     *
     * @param requestId The ID of the response to continue.
     * @throws RuntimeException if the operation fails.
     */
    public void continueResponse(String requestId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.ContinueResponse(requestId));
            System.out.println("Response continued: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing response: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network request with authentication challenge response.
     *
     * @param requestId              The ID of the request.
     * @param authChallengeResponse  The authentication challenge response.
     * @throws RuntimeException if the operation fails.
     */
    public void continueWithAuth(String requestId, AuthCredentials authChallengeResponse) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.ContinueWithAuth(requestId, authChallengeResponse));
            System.out.println("Request continued with authentication: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing with authentication: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fails a network request with the given request ID.
     *
     * @param requestId The ID of the request to fail.
     * @throws RuntimeException if the operation fails.
     */
    public void failRequest(String requestId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.FailRequest(requestId));
            System.out.println("Request failed: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error failing request: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Provides a custom response for a network request.
     *
     * @param requestId   The ID of the request to respond to.
     * @throws RuntimeException if the operation fails.
     */
    public void provideResponse(String requestId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.ProvideResponse(requestId));
            System.out.println("Response provided for request: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error providing response: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Removes a previously added intercept.
     *
     * @param interceptId The ID of the intercept to remove.
     * @throws RuntimeException if the operation fails.
     */
    public void removeIntercept(String interceptId) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.RemoveIntercept(interceptId));
            System.out.println("Intercept removed: " + interceptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing intercept: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets the cache behavior for the given context.
     *
     * @param cacheBehavior The cache behavior to set.
     *
     * @throws RuntimeException if the operation fails.
     */
    public void setCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new NetworkRequest.SetCacheBehavior(cacheBehavior));
        } catch (RuntimeException e) {
            System.out.println("Error setting cache behavior: " + e.getMessage());
            throw e;
        }
    }
}