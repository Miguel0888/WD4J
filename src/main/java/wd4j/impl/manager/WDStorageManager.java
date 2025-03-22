package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.dto.command.request.WDStorageRequest;
import wd4j.impl.dto.command.request.parameters.storage.CookieFilter;
import wd4j.impl.dto.command.request.parameters.storage.SetCookieParameters;
import wd4j.impl.dto.command.response.WDStorageResult;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WebSocketManager;

public class WDStorageManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDStorageManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves cookies for the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @return A JSON string containing the cookies.
     * @throws RuntimeException if the operation fails.
     */
    public WDStorageResult.GetCookieResult getCookies(WDBrowsingContext contextId) {
        return webSocketManager.sendAndWaitForResponse(
                new WDStorageRequest.GetCookies(contextId),
                WDStorageResult.GetCookieResult.class
        );
    }

    /**
     * Sets a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param cookie      The name and value of the cookie.
     * @throws RuntimeException if the operation fails.
     */
    public WDStorageResult.SetCookieResult setCookie(String contextId, SetCookieParameters.PartialCookie cookie) {
        return webSocketManager.sendAndWaitForResponse(
                new WDStorageRequest.SetCookie(contextId, cookie),
                WDStorageResult.SetCookieResult.class
        );
    }

    /**
     * Deletes a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param name      The name of the cookie to delete.
     * @throws RuntimeException if the operation fails.
     */
    public WDStorageResult.DeleteCookiesResult deleteCookie(String contextId, String name) {
        CookieFilter cookieFilter = new CookieFilter(name, null, null, null, null, null, null, null, null);

        return webSocketManager.sendAndWaitForResponse(
                new WDStorageRequest.DeleteCookies(contextId, cookieFilter),
                WDStorageResult.DeleteCookiesResult.class
        );
    }
}