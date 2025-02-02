package wd4j.impl.module;

import wd4j.impl.WebSocketImpl;
import wd4j.core.generic.Module;
import wd4j.impl.module.command.Network;

public class NetworkService implements Module {

    private final WebSocketImpl webSocketImpl;

    public NetworkService(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
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
     * @param urlPattern The URL pattern to intercept.
     * @throws RuntimeException if the operation fails.
     */
    public void addIntercept(String urlPattern) {
        try {
            webSocketImpl.sendAndWaitForResponse(new Network.AddIntercept(urlPattern));
            System.out.println("Intercept added for URL pattern: " + urlPattern);
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
            webSocketImpl.sendAndWaitForResponse(new Network.ContinueRequest(requestId));
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
            webSocketImpl.sendAndWaitForResponse(new Network.ContinueResponse(requestId));
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
    public void continueWithAuth(String requestId, String authChallengeResponse) {
        try {
            webSocketImpl.sendAndWaitForResponse(new Network.ContinueWithAuth(requestId, authChallengeResponse));
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
            webSocketImpl.sendAndWaitForResponse(new Network.FailRequest(requestId));
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
     * @param responseBody The custom response body.
     * @throws RuntimeException if the operation fails.
     */
    public void provideResponse(String requestId, String responseBody) {
        try {
            webSocketImpl.sendAndWaitForResponse(new Network.ProvideResponse(requestId, responseBody));
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
            webSocketImpl.sendAndWaitForResponse(new Network.RemoveIntercept(interceptId));
            System.out.println("Intercept removed: " + interceptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing intercept: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets the cache behavior for the given context.
     *
     * @param contextId The ID of the context.
     * @param cacheMode The cache mode to set (e.g., "no-store").
     * @throws RuntimeException if the operation fails.
     */
    public void setCacheBehavior(String contextId, String cacheMode) {
        try {
            webSocketImpl.sendAndWaitForResponse(new Network.SetCacheBehavior(contextId, cacheMode));
            System.out.println("Cache behavior set for context: " + contextId + " -> " + cacheMode);
        } catch (RuntimeException e) {
            System.out.println("Error setting cache behavior: " + e.getMessage());
            throw e;
        }
    }
}