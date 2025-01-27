package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;

import java.util.ArrayList;
import java.util.List;

public class Script implements Module {

    private final WebSocketConnection webSocketConnection;

    public Script(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds a preload script to the specified target.
     *
     * @param script The script to preload.
     * @param target The target to which the script is added.
     * @throws RuntimeException if the operation fails.
     */
    public void addPreloadScript(String script, String target) {
        try {
            webSocketConnection.send(new AddPreloadScript(script, target));
            System.out.println("Preload script added: " + script + " to target: " + target);
        } catch (RuntimeException e) {
            System.out.println("Error adding preload script: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Disowns the given handles in the specified context.
     *
     * @param contextId The ID of the context.
     * @param handles   The list of handles to disown.
     * @throws RuntimeException if the operation fails.
     */
    public void disown(String contextId, List<String> handles) {
        if (handles == null || handles.isEmpty()) {
            throw new IllegalArgumentException("Handles list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new Disown(contextId, handles));
            System.out.println("Handles disowned in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error disowning handles: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param target              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @throws RuntimeException if the operation fails.
     */
    public void callFunction(String functionDeclaration, String target, List<Object> arguments) {
        try {
            webSocketConnection.send(new CallFunction(functionDeclaration, target, arguments));
            System.out.println("Function called: " + functionDeclaration + " on target: " + target);
        } catch (RuntimeException e) {
            System.out.println("Error calling function: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param expression The expression to evaluate.
     * @param target     The target where the expression is evaluated.
     * @throws RuntimeException if the operation fails.
     */
    public void evaluate(String expression, String target) {
        try {
            webSocketConnection.send(new Evaluate(expression, target));
            System.out.println("Expression evaluated: " + expression + " on target: " + target);
        } catch (RuntimeException e) {
            System.out.println("Error evaluating expression: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the realms for the specified context.
     *
     * @param contextId The ID of the context.
     * @return A list of realm IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getRealms(String contextId) {
        try {
            String response = webSocketConnection.send(new GetRealms(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray realms = jsonResponse.getAsJsonObject("result").getAsJsonArray("realms");
            List<String> realmIds = new ArrayList<>();
            realms.forEach(realm -> realmIds.add(realm.getAsString()));
            System.out.println("Realms retrieved for context: " + contextId);
            return realmIds;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving realms: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Removes a preload script with the specified script ID.
     *
     * @param scriptId The ID of the script to remove.
     * @throws RuntimeException if the operation fails.
     */
    public void removePreloadScript(String scriptId) {
        try {
            webSocketConnection.send(new RemovePreloadScript(scriptId));
            System.out.println("Preload script removed: " + scriptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing preload script: " + e.getMessage());
            throw e;
        }
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddPreloadScript extends CommandImpl<AddPreloadScript.ParamsImpl> {

        public AddPreloadScript(String script, String target) {
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