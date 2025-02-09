package wd4j.impl.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.ScriptRequest;
import wd4j.impl.playwright.WebSocketImpl;

import java.util.ArrayList;
import java.util.List;

public class ScriptService implements Module {

    private final WebSocketImpl webSocketImpl;

    public ScriptService(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
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
            webSocketImpl.sendAndWaitForResponse(new ScriptRequest.AddPreloadScrip(script, target));
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
            webSocketImpl.sendAndWaitForResponse(new ScriptRequest.Disown(contextId, handles));
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
            webSocketImpl.sendAndWaitForResponse(new ScriptRequest.CallFunction(functionDeclaration, target, arguments));
            System.out.println("Function called: " + functionDeclaration + " on target: " + target);
        } catch (RuntimeException e) {
            System.out.println("Error calling function: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param contextId The ID of the context.
     * @throws RuntimeException if the operation fails.
     */
    public String evaluate(String script, String contextId) {
        return webSocketImpl.sendAndWaitForResponse(new ScriptRequest.Evaluate(script, contextId));
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
            String response = webSocketImpl.sendAndWaitForResponse(new ScriptRequest.GetRealms(contextId));
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
            webSocketImpl.sendAndWaitForResponse(new ScriptRequest.RemovePreloadScript(scriptId));
            System.out.println("Preload script removed: " + scriptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing preload script: " + e.getMessage());
            throw e;
        }
    }

}