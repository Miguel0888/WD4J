package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.core.generic.Command;

import java.util.ArrayList;
import java.util.List;

public class Script {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddPreloadScrip extends CommandImpl<AddPreloadScrip.ParamsImpl> {

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

    public static class Disown extends CommandImpl<Disown.ParamsImpl> {

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

    public static class CallFunction extends CommandImpl<CallFunction.ParamsImpl> {

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

    public static class Evaluate extends CommandImpl<Evaluate.ParamsImpl> {

        public Evaluate(String expression, String target) {
            super("script.evaluate", new ParamsImpl(expression, target));
        }

        public static class ParamsImpl implements Command.Params {
            private final String expression;
            private final String target;

            public ParamsImpl(String expression, String target) {
                if (expression == null || expression.isEmpty()) {
                    throw new IllegalArgumentException("Expression must not be null or empty.");
                }
                if (target == null || target.isEmpty()) {
                    throw new IllegalArgumentException("Target must not be null or empty.");
                }
                this.expression = expression;
                this.target = target;
            }
        }
    }

    public static class GetRealms extends CommandImpl<GetRealms.ParamsImpl> {

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

    public static class RemovePreloadScript extends CommandImpl<RemovePreloadScript.ParamsImpl> {

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