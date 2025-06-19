package de.bund.zrb.manager;

import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.command.request.WDScriptRequest;
import de.bund.zrb.command.response.WDEmptyResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.*;
import de.bund.zrb.api.WebSocketManager;

import java.util.List;

public class WDScriptManager implements WDModule {

    private final WebSocketManager webSocketManager;

    public WDScriptManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds a preload script for the entire session. It is functional the same as the evaluate command, but the script
     * will be re-evaluated on every new page load (=after the original realm was destroyed).
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
                new WDScriptRequest.AddPreloadScript(script, arguments),
                WDScriptResult.AddPreloadScriptResult.class
        );
    }

    /**
     * Adds a preload script to the specified target.
     *
     * @param script The script to preload.
     * @param arguments Contains only the ChannelValues that are passed to the script used for callback events.
     * @param browsingContexts The browsing contexts (aka. pages) to which the script is added

     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.AddPreloadScriptResult addPreloadScript(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> browsingContexts) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.AddPreloadScript(script, arguments, browsingContexts),
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
    public WDScriptResult.AddPreloadScriptResult addPreloadScript(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> WDBrowsingContexts, List<WDUserContext> WDUserContexts, String sandbox) {
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
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param target              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @param thisArg             The value of 'this' in the function.
     * @throws RuntimeException if the operation fails.
     */
    public <T> WDEvaluateResult callFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDLocalValue thisArg) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.CallFunction(functionDeclaration, awaitPromise, target, arguments, thisArg),
                WDEvaluateResult.class
        );
    }

    /**
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param target              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @param thisArg             The value of 'this' in the function.
     * @param resultOwnership     The ownership of the result: root = remote object (id only), none = local object if possible
     * @param serializationOptions The serialization options for the result.
     *
     * @throws RuntimeException if the operation fails.
     */
    public <T> WDEvaluateResult callFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDLocalValue thisArg, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.CallFunction(functionDeclaration, awaitPromise, target, arguments,
                        resultOwnership, serializationOptions, thisArg),
                WDEvaluateResult.class
        );
    }

    /**
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param target              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @param thisArg             The value of 'this' in the function.
     * @param resultOwnership     The ownership of the result: root = remote object (id only), none = local object if possible
     * @param serializationOptions The serialization options for the result.
     * @param userActivation Whether the script is executed in a user-activated context.
     *
     * @throws RuntimeException if the operation fails.
     */
    public <T> WDEvaluateResult callFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDLocalValue thisArg, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, boolean userActivation) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.CallFunction(functionDeclaration, awaitPromise, target, arguments,
                        resultOwnership, serializationOptions, thisArg, userActivation),
                WDEvaluateResult.class
        );
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param target    The target where the script is evaluated. See {@link WDTarget}.
     * @param awaitPromise Whether to wait for the promise to resolve.
     * @throws RuntimeException if the operation fails.
     */
    public WDEvaluateResult evaluate(String script, WDTarget target, boolean awaitPromise) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.Evaluate(script, target, awaitPromise),
                WDEvaluateResult.class
        );
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param target    The target where the script is evaluated. See {@link WDTarget}.
     * @param awaitPromise Whether to wait for the promise to resolve.
     * @param resultOwnership The ownership of the result: root = remote object (id only), none = local object if possible
     * @param serializationOptions The serialization options for the result.
     *
     * @throws RuntimeException if the operation fails.
     */
    public WDEvaluateResult evaluate(String script, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.Evaluate(script, target, awaitPromise, resultOwnership, serializationOptions),
                WDEvaluateResult.class
        );
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param target    The target where the script is evaluated. See {@link WDTarget}.
     * @param awaitPromise Whether to wait for the promise to resolve.
     * @param resultOwnership The ownership of the result: root = remote object (id only), none = local object if possible
     * @param serializationOptions The serialization options for the result.
     * @param userActivation Whether the script is executed in a user-activated context.
     *
     * @throws RuntimeException if the operation fails.
     */
    public WDEvaluateResult evaluate(String script, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, boolean userActivation) {
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.Evaluate(script, target, awaitPromise, resultOwnership, serializationOptions, userActivation),
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
     * Retrieves the realms for the specified context.
     *
     * @param context The ID of the context.
     * @param type The type of the realms to retrieve.
     *
     * @return A list of realm IDs.
     * @throws RuntimeException if the operation fails.
     */
    public WDScriptResult.GetRealmsResult getRealms(WDBrowsingContext context, WDRealmType type){
        return webSocketManager.sendAndWaitForResponse(
                new WDScriptRequest.GetRealms(context, type),
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // JS Functions available via CallFunction and SharedId given by the locateNodes Command
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated
    public void executeDomAction(String browsingContextId, String sharedId, DomAction action) {
        List<WDLocalValue> args = null;
                callFunction(
                        action.getFunctionDeclaration(),
                        false, // awaitPromise=false
                        new WDTarget.ContextTarget(new WDBrowsingContext(browsingContextId)),
                        args,
                        new WDRemoteReference.SharedReference(new WDSharedId(sharedId))
                );
    }

    @Deprecated
    public void executeDomAction(WDTarget.ContextTarget target, WDRemoteReference<?> remoteReference, DomAction action) {
        List<WDLocalValue> args = null;
        callFunction(
                action.getFunctionDeclaration(),
                false, // awaitPromise=false
                target,
                args,
                remoteReference
        );
    }

    @Deprecated
    public void executeDomAction(String browsingContextId, String sharedId, DomAction action, List<WDLocalValue> args) {
        callFunction(
                action.getFunctionDeclaration(),
                false, // awaitPromise=false
                new WDTarget.ContextTarget(new WDBrowsingContext(browsingContextId)),
                args,
                new WDRemoteReference.SharedReference(new WDSharedId(sharedId))
        );
    }

    @Deprecated
    public void executeDomAction(WDTarget.ContextTarget target, WDRemoteReference<?> remoteReference, DomAction action, List<WDLocalValue> args) {
        callFunction(
                action.getFunctionDeclaration(),
                false, // awaitPromise=false
                target,
                args,
                remoteReference
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated
    public WDEvaluateResult queryDomProperty(String browsingContextId, String sharedId, DomQuery domQuery) {
        WDEvaluateResult result = callFunction(
                domQuery.getFunctionDeclaration(),
                false, // awaitPromise=false
                new WDTarget.ContextTarget(new WDBrowsingContext(browsingContextId)),
                null,
                new WDRemoteReference.SharedReference(new WDSharedId(sharedId))
        );
        return result;
    }

    @Deprecated
    public WDEvaluateResult queryDomProperty(WDTarget.ContextTarget target, WDRemoteReference<?> remoteReference, DomQuery domQuery) {
        WDEvaluateResult result = callFunction(
                domQuery.getFunctionDeclaration(),
                false, // awaitPromise=false
                target,
                null,
                remoteReference
        );
        return result;
    }

    @Deprecated
    public WDEvaluateResult queryDomProperty(String browsingContextId, String sharedId, DomQuery domQuery, List<WDLocalValue> args) {
        WDEvaluateResult result = callFunction(
                domQuery.getFunctionDeclaration(),
                false, // awaitPromise=false
                new WDTarget.ContextTarget(new WDBrowsingContext(browsingContextId)),
                args,
                new WDRemoteReference.SharedReference(new WDSharedId(sharedId))
        );
        return result;
    }

    @Deprecated
    public WDEvaluateResult queryDomProperty(WDTarget.ContextTarget target, WDRemoteReference<?> remoteReference, DomQuery domQuery, List<WDLocalValue> args) {
        WDEvaluateResult result = callFunction(
                domQuery.getFunctionDeclaration(),
                false, // awaitPromise=false
                target,
                args,
                remoteReference
        );
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public enum DomAction {
        CLICK("function() { this.click(); }"),
        FOCUS("function() { this.focus(); }"),
        BLUR("function() { this.blur(); }"),
        INPUT("function(value) { this.value = value; this.dispatchEvent(new Event('input')); }"),
        CHANGE("function(value) { this.value = value; this.dispatchEvent(new Event('change')); }"),
        SELECT("function(value) { this.value = value; this.dispatchEvent(new Event('change')); }"), // ToDo: Check if this is really correct
        CHECK("function() { this.checked = true; this.dispatchEvent(new Event('change')); }"),
        UNCHECK("function() { this.checked = false; this.dispatchEvent(new Event('change')); }"), // ToDo: Check if this is really correct
        HOVER("function() { this.dispatchEvent(new MouseEvent('mouseover')); }"), // ToDo: Check if this is correct
        HIGHLIGHT("function() { this.style.outline = '2px solid red'; }"), // ToDo: Check if this is correct
        DRAG_AND_DROP( "function(target) { this.dispatchEvent(new DragEvent('dragstart')); target.dispatchEvent(new DragEvent('drop')); this.dispatchEvent(new DragEvent('dragend')); }"), // ToDo: Check if this is correct
        SCROLL_INTO_VIEW("function() { this.scrollIntoView(); }"), // ToDo: Check if this is correct
        TAP("function() { this.dispatchEvent(new MouseEvent('touchstart')); this.dispatchEvent(new MouseEvent('touchend')); }"), // ToDo: Check if this is correct
        PRESS_KEY("function(key) { this.dispatchEvent(new KeyboardEvent('keydown', { key: key })); this.dispatchEvent(new KeyboardEvent('keypress', { key: key })); this.dispatchEvent(new KeyboardEvent('keyup', { key: key })); }"), // ToDo: Check if this is correct
        CLEAR_INPUT("function() { this.value = ''; this.dispatchEvent(new Event('input')); }"), // ToDo: Check if this is correct
        SELECT_TEXT("function() { this.select(); }"), // ToDo: Check if this is correct

        SUBMIT("function() { this.submit(); }"), // ToDo: Check if this is correct
        RESET("function() { this.reset(); }"), // ToDo: Check if this is correct

        SCROLL_TO("function(x, y) { this.scrollTo(x, y); }"), // ToDo: Check if this is correct
        SCROLL_BY("function(x, y) { this.scrollBy(x, y); }"), // ToDo: Check if this is correct
        SCROLL_TOP("function() { this.scrollTop = 0; }"), // ToDo: Check if this is correct
        SCROLL_BOTTOM("function() { this.scrollTop = this.scrollHeight; }"), // ToDo: Check if this is correct
        SCROLL_LEFT("function() { this.scrollLeft = 0; }"), // ToDo: Check if this is correct
        SCROLL_RIGHT("function() { this.scrollLeft = this.scrollWidth; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT("function() { return this.scrollHeight; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH("function() { return this.scrollWidth; }"), // ToDo: Check if this is correct
        SCROLL_TOP_MAX("function() { return this.scrollHeight - this.clientHeight; }"), // ToDo: Check if this is correct
        SCROLL_LEFT_MAX("function() { return this.scrollWidth - this.clientWidth; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_MAX("function() { return this.scrollHeight - this.clientHeight; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_MAX("function() { return this.scrollWidth - this.clientWidth; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_OFFSET("function() { return this.scrollHeight - this.clientHeight - this.scrollTop; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_OFFSET("function() { return this.scrollWidth - this.clientWidth - this.scrollLeft; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_RATIO("function() { return this.scrollTop / (this.scrollHeight - this.clientHeight); }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_RATIO("function() { return this.scrollLeft / (this.scrollWidth - this.clientWidth); }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_RATIO_OFFSET("function() { return (this.scrollTop + this.clientHeight) / this.scrollHeight; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_RATIO_OFFSET("function() { return (this.scrollLeft + this.clientWidth) / this.scrollWidth; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_RATIO_CENTER("function() { return (this.scrollTop + this.clientHeight / 2) / this.scrollHeight; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_RATIO_CENTER("function() { return (this.scrollLeft + this.clientWidth / 2) / this.scrollWidth; }"), // ToDo: Check if this is correct
        SCROLL_HEIGHT_RATIO_CENTER_OFFSET("function() { return (this.scrollTop + this.clientHeight / 2) / this.scrollHeight; }"), // ToDo: Check if this is correct
        SCROLL_WIDTH_RATIO_CENTER_OFFSET("function() { return (this.scrollLeft + this.clientWidth / 2) / this.scrollWidth; }"), // ToDo: Check if this is correct

        WAIT_FOR("function() { return new Promise(resolve => { this.addEventListener('click', () => { resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_TIMEOUT("function(timeout) { return new Promise(resolve => { setTimeout(() => { resolve(); }, timeout); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_SELECTOR("function(selector) { return new Promise(resolve => { const element = document.querySelector(selector); if (element) { resolve(element); } else { const observer = new MutationObserver(() => { const element = document.querySelector(selector); if (element) { observer.disconnect(); resolve(element); } }); observer.observe(document.body, { childList: true, subtree: true }); } }); }"), // ToDo: Check if this is correct
        WAIT_FOR_SELECTOR_TIMEOUT("function(selector, timeout) { return new Promise(resolve => { const element = document.querySelector(selector); if (element) { resolve(element); } else { const observer = new MutationObserver(() => { const element = document.querySelector(selector); if (element) { observer.disconnect(); resolve(element); } }); observer.observe(document.body, { childList: true, subtree: true }); setTimeout(() => { observer.disconnect(); resolve(null); }, timeout); } }); }"), // ToDo: Check if this is correct
        WAIT_FOR_NAVIGATION("function() { return new Promise(resolve => { window.addEventListener('popstate', () => { resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_NAVIGATION_TIMEOUT("function(timeout) { return new Promise(resolve => { const timeoutId = setTimeout(() => { resolve(); }, timeout); window.addEventListener('popstate', () => { clearTimeout(timeoutId); resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_NETWORK("function() { return new Promise(resolve => { window.addEventListener('fetch', () => { resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_NETWORK_TIMEOUT("function(timeout) { return new Promise(resolve => { const timeoutId = setTimeout(() => { resolve(); }, timeout); window.addEventListener('fetch', () => { clearTimeout(timeoutId); resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_SCRIPT("function(script) { return new Promise(resolve => { const scriptElement = document.createElement('script'); scriptElement.textContent = script; scriptElement.onload = () => { resolve(); }; document.head.appendChild(scriptElement); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_SCRIPT_TIMEOUT("function(script, timeout) { return new Promise(resolve => { const scriptElement = document.createElement('script'); scriptElement.textContent = script; scriptElement.onload = () => { resolve(); }; document.head.appendChild(scriptElement); setTimeout(() => { resolve(); }, timeout); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_STYLE("function() { return new Promise(resolve => { window.addEventListener('load', () => { resolve(); }, { once: true }); }); }"), // ToDo: Check if this is correct
        WAIT_FOR_STYLE_TIMEOUT("function(timeout) { return new Promise(resolve => { const timeoutId = setTimeout(() => { resolve(); }, timeout); window.addEventListener('load', () => { clearTimeout(timeoutId); resolve(); }, { once: true }); }); }"); // ToDo: Check if this is correct

        private final String functionDeclaration;

        DomAction(String functionDeclaration) {
            this.functionDeclaration = functionDeclaration;
        }

        public String getFunctionDeclaration() {
            return functionDeclaration;
        }
    }

    public enum DomQuery {
        GET_INNER_TEXT("function() { return this.innerText; }"),
        GET_VALUE("function() { return this.value; }"),
        GET_PLACEHOLDER("function() { return this.placeholder; }"),
        GET_TAG_NAME("function() { return this.tagName.toLowerCase(); }"),
        GET_CSS_CLASS("function() { return this.className; }"),
        GET_ATTRIBUTES("function() { let attrs = {}; for (let attr of this.attributes) { attrs[attr.name] = attr.value; } return attrs; }"),
        IS_CHECKED("function() { return this.checked; }"),
        IS_ENABLED("function() { return !this.disabled; }"), // ToDo: Check if this is correct
        IS_EDITABLE("function() { return !this.readOnly; }"), // ToDo: Check if this is correct
        IS_VISIBLE("function() { return this.offsetParent !== null; }"), // ToDo: Check if this is correct
        IS_HIDDEN("function() { return this.offsetParent === null; }"), // ToDo: Check if this is correct
        IS_SELECTED("function() { return this.selected; }"),
        GET_ROLE("function() { return this.getAttribute('role'); }"),
        GET_BOUNDING_BOX("function() { const rect = this.getBoundingClientRect(); return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }; }"), // ToDo: Check if this is correct
        GET_TEXT_CONTENT( "function() { return this.textContent; }"); // ToDo: Check if this is correct

        private final String functionDeclaration;

        DomQuery(String functionDeclaration) {
            this.functionDeclaration = functionDeclaration;
        }

        public String getFunctionDeclaration() {
            return functionDeclaration;
        }
    }

}