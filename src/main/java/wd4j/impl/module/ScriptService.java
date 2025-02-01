package wd4j.impl.module;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Module;
import wd4j.impl.module.command.Script;

import java.util.ArrayList;
import java.util.List;

public class ScriptService implements Module {

    private final WebSocketConnection webSocketConnection;

    public ScriptService(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
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
            webSocketConnection.send(new Script.AddPreloadScrip(script, target));
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
            webSocketConnection.send(new Script.Disown(contextId, handles));
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
            webSocketConnection.send(new Script.CallFunction(functionDeclaration, target, arguments));
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
            webSocketConnection.send(new Script.Evaluate(expression, target));
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
            String response = webSocketConnection.send(new Script.GetRealms(contextId));
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
            webSocketConnection.send(new Script.RemovePreloadScript(scriptId));
            System.out.println("Preload script removed: " + scriptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing preload script: " + e.getMessage());
            throw e;
        }
    }

}