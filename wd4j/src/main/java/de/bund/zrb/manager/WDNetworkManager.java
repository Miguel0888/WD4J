package de.bund.zrb.manager;

import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.command.request.WDNetworkRequest;
import de.bund.zrb.command.request.parameters.network.AddInterceptParameters;
import de.bund.zrb.command.request.parameters.network.RemoveInterceptParameters;
import de.bund.zrb.command.request.parameters.network.SetCacheBehaviorParameters;
import de.bund.zrb.command.response.WDEmptyResult;
import de.bund.zrb.command.response.WDNetworkResult;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDAuthCredentials;
import de.bund.zrb.api.WDWebSocketManager;
import de.bund.zrb.type.network.WDUrlPattern;

import java.util.List;

public class WDNetworkManager implements WDModule {

    private final WDWebSocketManager WDWebSocketManager;

    public WDNetworkManager(WDWebSocketManager WDWebSocketManager) {
        this.WDWebSocketManager = WDWebSocketManager;
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
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDNetworkRequest.AddIntercept(phases),
                WDNetworkResult.AddInterceptResult.class
        );
    }

    public  WDNetworkResult.AddInterceptResult addIntercept(List<AddInterceptParameters.InterceptPhase> phases, List<WDBrowsingContext> ctxs, List<WDUrlPattern> patterns) {
        return WDWebSocketManager.sendAndWaitForResponse(
                new WDNetworkRequest.AddIntercept(phases, ctxs, patterns),
                WDNetworkResult.AddInterceptResult.class
        );
    }

    /**
     * Continues a previously intercepted request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void continueRequest(String requestId) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueRequest(requestId), WDEmptyResult.class);
    }

    /**
     * Continues a previously intercepted response.
     *
     * @param requestId The ID of the intercepted response.
     */
    public void continueResponse(String requestId) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueResponse(requestId), WDEmptyResult.class);
    }


    /**
     * Continues an authentication request with provided credentials or rejects it.
     *
     * @param requestId The ID of the intercepted authentication request.
     * @param authChallengeResponse The authentication challenge response.
     */
    public void continueWithAuth(String requestId, WDAuthCredentials authChallengeResponse) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ContinueWithAuth(requestId, authChallengeResponse), WDEmptyResult.class);
    }

    /**
     * Fails an intercepted network request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void failRequest(String requestId) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.FailRequest(requestId), WDEmptyResult.class);
    }

    /**
     * Provides a custom response to an intercepted request.
     *
     * @param requestId The ID of the intercepted request.
     */
    public void provideResponse(String requestId) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.ProvideResponse(requestId), WDEmptyResult.class);
    }

    /**
     * Removes a previously added network request interception rule.
     *
     * @param interceptId The ID of the intercept to remove.
     */
    public void removeIntercept(String interceptId) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.RemoveIntercept(interceptId), WDEmptyResult.class);
    }

    /**
     * Sets the cache behavior for the given context.
     *
     * @param cacheBehavior The cache behavior to set.
     */
    public void setCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior) {
        WDWebSocketManager.sendAndWaitForResponse(new WDNetworkRequest.SetCacheBehavior(cacheBehavior), WDEmptyResult.class);
    }

    // === UPDATED TO NEW SPECS ===

    /** Add a data collector. */
    public de.bund.zrb.type.network.WDCollector addDataCollector(java.util.List<de.bund.zrb.type.network.WDDataType> dataTypes, int maxEncodedDataSize) {
        de.bund.zrb.command.response.WDNetworkResult.AddDataCollectorResult result =
                WDWebSocketManager.sendAndWaitForResponse(
                        new de.bund.zrb.command.request.WDNetworkRequest.AddDataCollector(dataTypes, maxEncodedDataSize),
                        de.bund.zrb.command.response.WDNetworkResult.AddDataCollectorResult.class
                );
        return result.getCollector();
    }

    /** Convenience: Add a response body collector for all contexts. */
    public de.bund.zrb.type.network.WDCollector addResponseBodyCollector(int maxEncodedDataSize) {
        java.util.List<de.bund.zrb.type.network.WDDataType> types = java.util.Collections.singletonList(de.bund.zrb.type.network.WDDataType.RESPONSE);
        return addDataCollector(types, maxEncodedDataSize);
    }

    /** Get collected data (bytes) for a request. Set disown=true to release association. */
    public de.bund.zrb.type.network.WDBytesValue getData(de.bund.zrb.type.network.WDDataType dataType,
                                                         de.bund.zrb.type.network.WDRequest request,
                                                         de.bund.zrb.type.network.WDCollector collector,
                                                         java.lang.Boolean disown) {
        de.bund.zrb.command.response.WDNetworkGetDataResult result =
                WDWebSocketManager.sendAndWaitForResponse(
                        new de.bund.zrb.command.request.WDNetworkRequest.GetData(
                                new de.bund.zrb.command.request.parameters.network.GetDataParameters(dataType, collector, disown, request)
                        ),
                        de.bund.zrb.command.response.WDNetworkGetDataResult.class
                );
        return result.getBytes();
    }

    /** Disown previously collected data for a given request and collector. */
    public void disownData(de.bund.zrb.type.network.WDDataType dataType,
                           de.bund.zrb.type.network.WDCollector collector,
                           de.bund.zrb.type.network.WDRequest request) {
        WDWebSocketManager.sendAndWaitForResponse(
                new de.bund.zrb.command.request.WDNetworkRequest.DisownData(dataType, collector, request),
                de.bund.zrb.command.response.WDEmptyResult.class
        );
    }

    /** Remove a data collector. */
    public void removeDataCollector(de.bund.zrb.type.network.WDCollector collector) {
        WDWebSocketManager.sendAndWaitForResponse(
                new de.bund.zrb.command.request.WDNetworkRequest.RemoveDataCollector(collector),
                de.bund.zrb.command.response.WDEmptyResult.class
        );
    }

    /** Set extra headers for matching contexts/userContexts. */
    public void setExtraHeaders(java.util.List<de.bund.zrb.type.network.WDHeader> headers) {
        WDWebSocketManager.sendAndWaitForResponse(
                new de.bund.zrb.command.request.WDNetworkRequest.SetExtraHeaders(headers),
                de.bund.zrb.command.response.WDEmptyResult.class
        );
    }
}