package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.script.*;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
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
    public static class AddPreloadScript extends CommandImpl<AddPreloadScriptParameters> implements CommandData {
        public AddPreloadScript(String script) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script));
        }
        public AddPreloadScript(String script, List<ChannelValue> arguments, List<BrowsingContext> browsingContexts, List<UserContext> userContexts, String sandbox) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, arguments, browsingContexts, userContexts, sandbox));
        }
    }

    /**
     * The script.disown command disowns the given handles. This does not guarantee the handled object will be garbage
     * collected, as there can be other handles or strong ECMAScript references.
     */
    public static class Disown extends CommandImpl<DisownParameters> implements CommandData {
        public Disown(List<Handle> handles, Target target) {
            super("script.disown", new DisownParameters(handles, target));
        }
    }

    /**
     * The script.callFunction command calls a provided function with given arguments in a given realm.
     *
     * RealmInfo can be either a realm or a navigable.
     * @param <T>
     */
    public static class CallFunction<T> extends CommandImpl<CallFunctionParameters> implements CommandData {
        public CallFunction(String functionDeclaration, boolean awaitPromise, Target target) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, Target target, List<LocalValue<T>> arguments) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target, arguments, null, null, false));
        }
        public CallFunction(String functionDeclaration, boolean awaitPromise, Target target, List<LocalValue<T>> arguments, SerializationOptions serializationOptions, LocalValue thisObject, boolean userActivation) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, awaitPromise, target, arguments, serializationOptions, thisObject, userActivation));
        }
    }

    /**
     * The script.evaluate command evaluates a provided script in a given realm. For convenience a navigable can be
     * provided in place of a realm, in which case the realm used is the realm of the browsing context’s active document.
     *
     * The method returns the value of executing the provided script, unless it returns a promise and awaitPromise is
     * true, in which case the resolved value of the promise is returned.
      */
    public static class Evaluate extends CommandImpl<EvaluateParameters> implements CommandData {
        public Evaluate(String expression, Target target, boolean awaitPromise) {
            super("script.evaluate", new EvaluateParameters(expression, target, awaitPromise));
        }
        public Evaluate(String expression, Target target, boolean awaitPromise, ResultOwnership resultOwnership, SerializationOptions serializationOptions, boolean userActivation) {
            super("script.evaluate", new EvaluateParameters(expression, target, awaitPromise, resultOwnership, serializationOptions, userActivation));
        }
    }

    /**
     * The script.getRealms command returns a list of all realms, optionally filtered to realms of a specific type, or
     * to the realm associated with a navigable’s active document.
     */
    public static class GetRealms extends CommandImpl<GetRealmsParameters> implements CommandData {
        public GetRealms(String contextId) {
            super("script.getRealms", new GetRealmsParameters(new BrowsingContext(contextId), null));
        }
        public GetRealms(BrowsingContext context) {
            super("script.getRealms", new GetRealmsParameters(context, null));
        }
        public GetRealms() {
            super("script.getRealms", new GetRealmsParameters());
        }
        public GetRealms(BrowsingContext browsingContext, RealmType type) {
            super("script.getRealms", new GetRealmsParameters(browsingContext, type));
        }
    }

    /**
     * The script.removePreloadScript command removes a preload script.
     */
    public static class RemovePreloadScript extends CommandImpl<RemovePreloadScriptParameters> implements CommandData {
        public RemovePreloadScript(String scriptId) {
            super("script.removePreloadScript", new RemovePreloadScriptParameters(new PreloadScript(scriptId)));
        }
        public RemovePreloadScript(PreloadScript script) {
            super("script.removePreloadScript", new RemovePreloadScriptParameters(script));
        }
    }
}