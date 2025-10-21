package de.bund.zrb.util;

import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.type.script.*;

import java.util.List;

/**
 * Utility-Klasse für Operationen auf WDRemoteValue-Instanzen.
 */
public final class ScriptUtils {

    private ScriptUtils() {
        // Prevent instantiation
    }


    /**
     * Gibt die SharedReference für ein RemoteValue des Typs "node" zurück.
     *
     * @param remoteValue das RemoteValue, z. B. aus einem EvaluateResult
     * @return die SharedReference, falls möglich
     * @throws IllegalArgumentException wenn der Typ nicht "node" ist oder notwendige Felder fehlen
     */
    public static WDRemoteReference.SharedReference sharedRef(WDRemoteValue remoteValue) {
        if (remoteValue == null) {
            throw new IllegalArgumentException("remoteValue must not be null");
        }

        if (!"node".equals(remoteValue.getType())) {
            throw new IllegalArgumentException("Expected a remote value of type 'node', but got: " + remoteValue.getType());
        }

        if (!(remoteValue instanceof WDRemoteValue.NodeRemoteValue)) {
            throw new IllegalArgumentException("Expected NodeRemoteValue, but got: " + remoteValue.getClass().getSimpleName());
        }

        WDRemoteValue.NodeRemoteValue node = (WDRemoteValue.NodeRemoteValue) remoteValue;
        WDHandle handle = node.getHandle();
        if (node.getSharedId() == null || handle == null) {
            throw new IllegalStateException("NodeRemoteValue is missing sharedId or handle");
        }

        return new WDRemoteReference.SharedReference(node.getSharedId(), handle);
    }

    /**
     * Prüft, ob ein RemoteValue einen Node repräsentiert.
     *
     * @param value das zu prüfende Value
     * @return true, wenn Typ "node", sonst false
     */
    public static boolean isNode(WDRemoteValue value) {
        return value instanceof WDRemoteValue.NodeRemoteValue;
    }

    /**
     * Gibt das Handle eines RemoteValue zurück, sofern vorhanden.
     *
     * @param value das zu prüfende Value
     * @return das Handle oder null
     */
    public static WDHandle getHandle(WDRemoteValue value) {
        if (value instanceof WDRemoteValue.BaseRemoteValue) {
            return ((WDRemoteValue.BaseRemoteValue) value).getHandle();
        }
        return null;
    }


    /**
     * Calls a JavaScript function with the given declaration on a remote reference (usually an element).
     *
     * @param functionDeclaration The JavaScript function to execute (e.g. "function() { return this.innerText; }").
     * @param target              The context target (browsing context).
     * @param remoteRef           The remote reference to use as `this`.
     * @param arguments           Optional function arguments (may be null).
     * @return The evaluation result.
     */
    public static WDEvaluateResult evaluateDomFunction(
            WDScriptManager scriptManager,
            String functionDeclaration,
            WDTarget target,
            WDRemoteReference<?> remoteRef,
            List<WDLocalValue> arguments
    ) {
        return scriptManager.callFunction(
                functionDeclaration,
                true, // awaitPromise
                target,
                arguments,
                remoteRef,
                WDResultOwnership.NONE,
                null
        );
    }

    public static String wrapExpressionAsFunction(String expression) {
        return expression.trim().startsWith("function")
                ? expression
                : "function(element) { return " + expression + "; }";
    }

}
