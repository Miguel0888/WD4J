package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WDScriptRequest;
import wd4j.impl.webdriver.command.request.parameters.script.AddPreloadScriptParameters;
import wd4j.impl.webdriver.command.response.WDEmptyResult;
import wd4j.impl.webdriver.command.response.WDScriptResult;
import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.*;
import wd4j.impl.websocket.WebSocketManager;

import java.util.List;

public class WDScriptManager implements WDModule {

    private final WebSocketManager webSocketManager;

    private static volatile WDScriptManager instance;

    private WDScriptManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    /**
     * Gibt die Singleton-Instanz von WDScriptManager zurück.
     *
     * @return Singleton-Instanz von WDScriptManager.
     */
    public static WDScriptManager getInstance() {
        if (instance == null) {
            synchronized (WDScriptManager.class) {
                if (instance == null) {
                    instance = new WDScriptManager(WebSocketManager.getInstance());
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
     * Adds a preload script for the entire session.
     *
     * @param script The script to preload.
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.AddPreloadScriptResult addPreloadScript(String script) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.AddPreloadScript(script),
                WDScriptResult.AddPreloadScriptResult.class
        );
    }

    /**
     * Adds a preload script for the entire session.
     *
     * @param script The script to preload.
     * @param arguments Contains only the ChannelValues that are passed to the script used for callback events.
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.AddPreloadScriptResult addPreloadScript(String script, List<WDChannelValue> arguments) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.AddPreloadScript(script),
                WDScriptResult.AddPreloadScriptResult.class
        );
    }

    /**
     * Adds a preload script to the specified target.
     *
     * @param script The script to preload.
     * @param arguments Contains only the ChannelValues that are passed to the script used for callback events.
     * @param WDBrowsingContexts The browsing contexts (aka. pages) to which the script is added
     * @param WDUserContexts The user contexts to which the script is added
     * @param sandbox The sandbox in which the script is executed

     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.AddPreloadScriptResult addPreloadScriptString(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> WDBrowsingContexts, List<WDUserContext> WDUserContexts, String sandbox) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.AddPreloadScript(script, arguments, WDBrowsingContexts, WDUserContexts, sandbox),
                WDScriptResult.AddPreloadScriptResult.class
        );
    }

    /**
     * Adds a preload script to the specified target.
     *
     * @param script The script to preload.
     * @param context The browsing context (aka. page) to which the script is added
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.AddPreloadScriptResult addPreloadScript(String script, String context) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.AddPreloadScript(script, context), // ToDo: Improve this
                WDScriptResult.AddPreloadScriptResult.class
        );
    }

    /**
     * Disowns the given handles in the specified context.
     *
     * @param WDTarget The ID of the context.
     * @param WDHandles   The list of handles to disown.
     * @throws RuntimeException if the operation fails.
     */
    public void disown(List<WDHandle> WDHandles, WDTarget WDTarget) {
        if (WDHandles == null || WDHandles.isEmpty()) {
            throw new IllegalArgumentException("Handles list must not be null or empty.");
        }

        webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.Disown(WDHandles, WDTarget),
                WDEmptyResult.class
        );
    }

    /**
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param target              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @throws RuntimeException if the operation fails.
     */
    public <T> WDEvaluateResult callFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.CallFunction(functionDeclaration, awaitPromise, target, arguments),
                WDEvaluateResult.class
        );
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param wdTarget    The target where the script is evaluated. See {@link WDTarget}.
     * @throws RuntimeException if the operation fails.
     */
    public WDEvaluateResult evaluate(String script, WDTarget wdTarget, boolean awaitPromise) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.Evaluate(script, wdTarget, awaitPromise),
                WDEvaluateResult.class
        );
    }

    /**
     * Retrieves all available realms (= JavaScript Threads) for the current session.
     *
     * @return A list of realm IDs.
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.GetRealmsResult getRealms() {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.GetRealms(),
                WDScriptResult.GetRealmsResult.class
        );
    }

    /**
     * Retrieves the realms for the specified context.
     *
     * @param context The ID of the context.
     * @return A list of realm IDs.
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.GetRealmsResult getRealms(WDBrowsingContext context) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.GetRealms(context),
                WDScriptResult.GetRealmsResult.class
        );
    }

    /**
     * Removes a preload script with the specified script ID.
     *
     * @param scriptId The ID of the script to remove.
     * @throws RuntimeException if the operation fails.
     */
    public void removePreloadScript(String scriptId) {
        webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.RemovePreloadScript(scriptId),
                WDEmptyResult.class
        );
    }
}