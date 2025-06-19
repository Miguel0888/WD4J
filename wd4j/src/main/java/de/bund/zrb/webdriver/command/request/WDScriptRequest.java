package de.bund.zrb.webdriver.command.request;

import de.bund.zrb.markerInterfaces.WDCommandData;
import de.bund.zrb.webdriver.command.request.helper.WDCommandImpl;
import de.bund.zrb.webdriver.type.browser.WDUserContext;
import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.webdriver.command.request.parameters.script.*;
import de.bund.zrb.webdriver.type.script.*;

import java.util.Collections;
import java.util.List;

public class WDScriptRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The script.addPreloadScript command adds a preload script.
     *
     * A Preload script is one which runs on creation of a new Window, before any author-defined script have run.
     *
     * A BiDi session has a preload script map which is a map in which the keys are UUIDs, and the values are structs
     * with an item named function declaration, which is a string, an item named arguments, which is a list, an item
     * named contexts, which is a list or null, an item named sandbox, which is a string or null, and an item named
     * user contexts, which is a set.
     */
    public static class AddPreloadScript extends WDCommandImpl<AddPreloadScriptParameters> implements WDCommandData {
        public AddPreloadScript(String script) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script)); // global script
        }
        public AddPreloadScript(String script, List<WDChannelValue> arguments) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, arguments));
        }
        public AddPreloadScript(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> browsingContexts) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, arguments, browsingContexts));
        }
        public AddPreloadScript(String script, String context) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, null,
                    Collections.singletonList(new WDBrowsingContext(context)))); // script for a specific context only
        }
        public AddPreloadScript(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> browsingContexts, List<WDUserContext> userContexts, String sandbox) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, arguments, browsingContexts, userContexts, sandbox));
        }
    }

    /**
     * The script.disown command disowns the given handles. This does not guarantee the handled object will be garbage
     * collected, as there can be other handles or strong ECMAScript references.
     */
    public static class Disown extends WDCommandImpl<DisownParameters> implements WDCommandData {
        public Disown(List<WDHandle> WDHandles, WDTarget WDTarget) {
            super("script.disown", new DisownParameters(WDHandles, WDTarget));
        }
    }

    /**
     * The script.callFunction command calls a provided function with given arguments in a given realm.
     *
     * RealmInfo can be either a realm or a navigable.
     * @param <T>
     */
    public static class CallFunction extends WDCommandImpl<CallFunctionParameters> implements WDCommandData {
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget target) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target, arguments));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDLocalValue thisObject) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target, arguments, thisObject));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, WDLocalValue thisObject) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target,
                    arguments, resultOwnership, serializationOptions, thisObject));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, WDLocalValue thisObject, boolean userActivation) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target,
                    arguments, resultOwnership, serializationOptions, thisObject, userActivation));
        }
    }

    /**
     * The script.evaluate command evaluates a provided script in a given realm. For convenience a navigable can be
     * provided in place of a realm, in which case the realm used is the realm of the browsing context’s active document.
     *
     * The method returns the value of executing the provided script, unless it returns a promise and awaitPromise is
     * true, in which case the resolved value of the promise is returned.
      */
    public static class Evaluate extends WDCommandImpl<EvaluateParameters> implements WDCommandData {
        public Evaluate(String expression, WDTarget target, boolean awaitPromise) {
            super("script.evaluate", new EvaluateParameters(expression, target, awaitPromise));
        }
        public Evaluate(String expression, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions) {
            super("script.evaluate", new EvaluateParameters(expression, target, awaitPromise, resultOwnership, serializationOptions));
        }
        public Evaluate(String expression, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, boolean userActivation) {
            super("script.evaluate", new EvaluateParameters(expression, target, awaitPromise, resultOwnership, serializationOptions, userActivation));
        }
    }

    /**
     * The script.getRealms command returns a list of all realms, optionally filtered to realms of a specific type, or
     * to the realm associated with a navigable’s active document.
     */
    public static class GetRealms extends WDCommandImpl<GetRealmsParameters> implements WDCommandData {
        public GetRealms(String contextId) {
            super("script.getRealms", new GetRealmsParameters(new WDBrowsingContext(contextId), null));
        }
        public GetRealms(WDBrowsingContext context) {
            super("script.getRealms", new GetRealmsParameters(context, null));
        }
        public GetRealms() {
            super("script.getRealms", new GetRealmsParameters());
        }
        public GetRealms(WDBrowsingContext browsingContext, WDRealmType type) {
            super("script.getRealms", new GetRealmsParameters(browsingContext, type));
        }
    }

    /**
     * The script.removePreloadScript command removes a preload script.
     */
    public static class RemovePreloadScript extends WDCommandImpl<RemovePreloadScriptParameters> implements WDCommandData {
        public RemovePreloadScript(String scriptId) {
            super("script.removePreloadScript", new RemovePreloadScriptParameters(new WDPreloadScript(scriptId)));
        }
        public RemovePreloadScript(WDPreloadScript script) {
            super("script.removePreloadScript", new RemovePreloadScriptParameters(script));
        }
    }
}