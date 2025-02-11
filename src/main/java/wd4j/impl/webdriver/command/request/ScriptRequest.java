package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.websocket.Command;

import java.util.ArrayList;
import java.util.List;

public class ScriptRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddPreloadScrip extends CommandImpl<AddPreloadScrip.ParamsImpl> implements CommandData {

        public AddPreloadScrip(String script, String target) {
            super("script.addPreloadScript", new ParamsImpl(script, target));
        }

        public static class ParamsImpl implements Command.Params {
            private final String script;
            private final String target;

            public ParamsImpl(String script, String target) {
                if (script == null || script.isEmpty()) {
                    throw new IllegalArgumentException("Script must not be null or empty.");
                }
                if (target == null || target.isEmpty()) {
                    throw new IllegalArgumentException("Target must not be null or empty.");
                }
                this.script = script;
                this.target = target;
            }
        }
    }

    public static class Disown extends CommandImpl<Disown.ParamsImpl> implements CommandData {

        public Disown(String contextId, List<String> handles) {
            super("script.disown", new ParamsImpl(contextId, handles));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final List<String> handles;

            public ParamsImpl(String contextId, List<String> handles) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (handles == null || handles.isEmpty()) {
                    throw new IllegalArgumentException("Handles list must not be null or empty.");
                }
                this.context = contextId;
                this.handles = handles;
            }
        }
    }

    public static class CallFunction extends CommandImpl<CallFunction.ParamsImpl> implements CommandData {

        public CallFunction(String functionDeclaration, String target, List<Object> arguments) {
            super("script.callFunction", new ParamsImpl(functionDeclaration, target, arguments));
        }

        public static class ParamsImpl implements Command.Params {
            private final String functionDeclaration;
            private final String target;
            private final List<Object> arguments;

            public ParamsImpl(String functionDeclaration, String target, List<Object> arguments) {
                if (functionDeclaration == null || functionDeclaration.isEmpty()) {
                    throw new IllegalArgumentException("Function declaration must not be null or empty.");
                }
                if (target == null || target.isEmpty()) {
                    throw new IllegalArgumentException("Target must not be null or empty.");
                }
                this.functionDeclaration = functionDeclaration;
                this.target = target;
                this.arguments = arguments != null ? arguments : new ArrayList<>();
            }
        }
    }

    public static class Evaluate extends CommandImpl<Evaluate.ParamsImpl> implements CommandData {
        public Evaluate(String script, String contextId) {
            super("script.evaluate", new ParamsImpl(script, contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String expression;
            private final String context;
            private final String resultOwnership = "root"; // Playwright erwartet root Ownership

            public ParamsImpl(String script, String contextId) {
                if (script == null || script.isEmpty()) {
                    throw new IllegalArgumentException("Script must not be null or empty.");
                }
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.expression = script;
                this.context = contextId;
            }
        }
    }


    public static class GetRealms extends CommandImpl<GetRealms.ParamsImpl> implements CommandData {

        public GetRealms(String contextId) {
            super("script.getRealms", new ParamsImpl(contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;

            public ParamsImpl(String contextId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
            }
        }
    }

    public static class RemovePreloadScript extends CommandImpl<RemovePreloadScript.ParamsImpl> implements CommandData {

        public RemovePreloadScript(String scriptId) {
            super("script.removePreloadScript", new ParamsImpl(scriptId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String script;

            public ParamsImpl(String scriptId) {
                if (scriptId == null || scriptId.isEmpty()) {
                    throw new IllegalArgumentException("Script ID must not be null or empty.");
                }
                this.script = scriptId;
            }
        }
    }

}