package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.script.*;
import wd4j.impl.websocket.Command;

import java.util.ArrayList;
import java.util.List;

public class ScriptRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddPreloadScript extends CommandImpl<AddPreloadScriptParameters> implements CommandData {
        public AddPreloadScript(String script, String target) {
            super("script.addPreloadScript", new AddPreloadScriptParameters(script, target));
        }
    }

    public static class Disown extends CommandImpl<DisownParameters> implements CommandData {
        public Disown(String contextId, List<String> handles) {
            super("script.disown", new DisownParameters(contextId, handles));
        }
    }

    public static class CallFunction extends CommandImpl<CallFunctionParameters> implements CommandData {
        public CallFunction(String functionDeclaration, String target, List<Object> arguments) {
            super("script.callFunction", new CallFunctionParameters(functionDeclaration, target, arguments));
        }
    }

    public static class Evaluate extends CommandImpl<EvaluateParameters> implements CommandData {
        public Evaluate(String script, String contextId) {
            super("script.evaluate", new EvaluateParameters(script, contextId));
        }
    }

    public static class GetRealms extends CommandImpl<GetRealmsParameters> implements CommandData {
        public GetRealms(String contextId) {
            super("script.getRealms", new GetRealmsParameters(contextId));
        }
    }

    public static class RemovePreloadScript extends CommandImpl<RemovePreloadScriptParameters> implements CommandData {
        public RemovePreloadScript(String scriptId) {
            super("script.removePreloadScript", new RemovePreloadScriptParameters(scriptId));
        }
    }

}