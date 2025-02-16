package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.webdriver.command.request.helper.WDCommandImpl;
import wd4j.impl.webdriver.command.request.parameters.script.*;
import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.*;

import java.util.List;

public class ScriptRequest {

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
            super("script.addPreloadScript", new AddPreloadScriptParameters(script));
        }
        public AddPreloadScript(String script, List<WDChannelValue> arguments, List<WDBrowsingContext> WDBrowsingContexts, List<WDUserContext> WDUserContexts, String sandbox) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, arguments, WDBrowsingContexts, WDUserContexts, sandbox));
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
    public static class CallFunction<T> extends WDCommandImpl<CallFunctionParameters> implements WDCommandData {
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, WDTarget));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget, List<WDLocalValue<T>> arguments) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, WDTarget, arguments, null, null, false));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget, List<WDLocalValue<T>> arguments, WDSerializationOptions WDSerializationOptions, WDLocalValue thisObject, boolean userActivation) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, WDTarget, arguments, WDSerializationOptions, thisObject, userActivation));
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
        public Evaluate(String expression, WDTarget WDTarget, boolean awaitPromise) {
            super("script.evaluate", new EvaluateParameters(expression, WDTarget, awaitPromise));
        }
        public Evaluate(String expression, WDTarget WDTarget, boolean awaitPromise, WDResultOwnership WDResultOwnership, WDSerializationOptions WDSerializationOptions, boolean userActivation) {
            super("script.evaluate", new EvaluateParameters(expression, WDTarget, awaitPromise, WDResultOwnership, WDSerializationOptions, userActivation));
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
        public GetRealms(WDBrowsingContext WDBrowsingContext, WDRealmType type) {
            super("script.getRealms", new GetRealmsParameters(WDBrowsingContext, type));
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