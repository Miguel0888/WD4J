package wd4j.impl.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.ScriptRequest;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDHandle;
import wd4j.impl.webdriver.type.script.WDLocalValue;
import wd4j.impl.webdriver.type.script.WDTarget;
import wd4j.impl.websocket.WebSocketManager;

import java.util.ArrayList;
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
     * Adds a preload script to the specified target.
     *
     * @param script The script to preload.
     * @param target The target to which the script is added.
     * @throws RuntimeException if the operation fails.
     */
    public void addPreloadScript(String script, String target) {
        try {
            webSocketManager.sendAndWaitForResponse(new ScriptRequest.AddPreloadScript(script), String.class);
            System.out.println("Preload script added: " + script + " to target: " + target);
        } catch (RuntimeException e) {
            System.out.println("Error adding preload script: " + e.getMessage());
            throw e;
        }
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

        try {
            webSocketManager.sendAndWaitForResponse(new ScriptRequest.Disown(WDHandles, WDTarget), String.class);
            System.out.println("Handles disowned in target: " + WDTarget);
        } catch (RuntimeException e) {
            System.out.println("Error disowning handles: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Calls a function on the specified target with the given arguments.
     *
     * @param functionDeclaration The function to call.
     * @param WDTarget              The target where the function is called.
     * @param arguments           The arguments to pass to the function.
     * @throws RuntimeException if the operation fails.
     */
    public <T> void callFunction(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget, List<WDLocalValue<T>> arguments) {
        try {
            webSocketManager.sendAndWaitForResponse(new ScriptRequest.CallFunction<>(functionDeclaration, awaitPromise, WDTarget, arguments), String.class);
            System.out.println("Function called: " + functionDeclaration + " on target: " + WDTarget);
        } catch (RuntimeException e) {
            System.out.println("Error calling function: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Evaluates the given expression in the specified target.
     *
     * @param script    The script to evaluate.
     * @param WDTarget    The target where the script is evaluated. See {@link WDTarget}.
     * @throws RuntimeException if the operation fails.
     */
    public String evaluate(String script, WDTarget WDTarget, boolean awaitPromise) {
        return webSocketManager.sendAndWaitForResponse(new ScriptRequest.Evaluate(script, WDTarget, awaitPromise), String.class);
    }


    /**
     * Retrieves the realms for the specified context.
     *
     * @param context The ID of the context.
     * @return A list of realm IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getRealms(WDBrowsingContext context) {
        try {
            String response = webSocketManager.sendAndWaitForResponse(new ScriptRequest.GetRealms(context), String.class);
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray realms = jsonResponse.getAsJsonObject("result").getAsJsonArray("realms");
            List<String> realmIds = new ArrayList<>();
            realms.forEach(realm -> realmIds.add(realm.getAsString()));
            System.out.println("Realms retrieved for context: " + context);
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
            webSocketManager.sendAndWaitForResponse(new ScriptRequest.RemovePreloadScript(scriptId), String.class);
            System.out.println("Preload script removed: " + scriptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing preload script: " + e.getMessage());
            throw e;
        }
    }

}