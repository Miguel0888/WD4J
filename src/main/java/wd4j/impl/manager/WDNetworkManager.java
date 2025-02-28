package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WDNetworkRequest;
import wd4j.impl.webdriver.command.request.parameters.network.AddInterceptParameters;
import wd4j.impl.webdriver.command.request.parameters.network.SetCacheBehaviorParameters;
import wd4j.impl.webdriver.command.response.WDEmptyResult;
import wd4j.impl.webdriver.command.response.WDNetworkResult;
import wd4j.impl.webdriver.type.network.WDAuthCredentials;
import wd4j.impl.websocket.WebSocketManager;

import java.util.List;

public class WDNetworkManager implements WDModule {

    private static WDNetworkManager instance;
    private final WebSocketManager webSocketManager;

    private WDNetworkManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    /**
     * Gibt die Singleton-Instanz von WDNetworkManager zurück.
     *
     * @return Singleton-Instanz von WDNetworkManager.
     */
    public static WDNetworkManager getInstance() {
        if (instance == null) {
            synchronized (WDScriptManager.class) {
                if (instance == null) {
                    instance = new WDNetworkManager(WebSocketManager.getInstance());
                }
            }
        }
        return instance;
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
     * @return The intercept ID of the added rule.
     */
    public WDNetworkResult.AddInterceptResult addIntercept(List<AddInterceptParameters.InterceptPhase> phases) {
        return webSocketManager.sendAndWaitForResponse(
                new WDNetworkRequest.AddIntercept(phases),
                WDNetworkResult.AddInterceptResult.class
        );
    }

    /**
     * Continues a previously intercepted request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void continueRequest(String requestId) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueRequest(requestId), WDEmptyResult.class);
    }

    /**
     * Continues a previously intercepted response.
     *
     * @param requestId The ID of the intercepted response.
     */
    public void continueResponse(String requestId) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueResponse(requestId), WDEmptyResult.class);
    }


    /**
     * Continues an authentication request with provided credentials or rejects it.
     *
     * @param requestId The ID of the intercepted authentication request.
     * @param authChallengeResponse The authentication challenge response.
     */
    public void continueWithAuth(String requestId, WDAuthCredentials authChallengeResponse) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueWithAuth(requestId, authChallengeResponse), WDEmptyResult.class);
    }

    /**
     * Fails an intercepted network request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void failRequest(String requestId) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.FailRequest(requestId), WDEmptyResult.class);
    }

    /**
     * Provides a custom response to an intercepted request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void provideResponse(String requestId) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ProvideResponse(requestId), WDEmptyResult.class);
    }

    /**
     * Removes a previously added network request interception rule.
     *
     * @param interceptId The ID of the intercept to remove.
     */
    public void removeIntercept(String interceptId) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.RemoveIntercept(interceptId), WDEmptyResult.class);
    }

    /**
     * Sets the cache behavior for the given context.
     *
     * @param cacheBehavior The cache behavior to set.
     */
    public void setCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior) {
        webSocketManager.sendAndWaitForResponse(new WDNetworkRequest.SetCacheBehavior(cacheBehavior), WDEmptyResult.class);
    }
}