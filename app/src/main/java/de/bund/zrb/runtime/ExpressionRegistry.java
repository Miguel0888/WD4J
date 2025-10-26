package de.bund.zrb.runtime;

import java.util.List;
import java.util.Optional;

/**
 * Provide a registry of named expression functions.
 *
 * Intent:
 * - Store and retrieve code snippets / definitions for logical functions
 *   like otp(), wrap(), etc.
 * - Evaluate a function on demand with parameters.
 *
 * Scope:
 * - This interface abstracts storage and evaluation strategy.
 * - UI (ExpressionEditorPanel) and resolver code both depend on this,
 *   not on a concrete implementation.
 */
public interface ExpressionRegistry {

    /**
     * Return all known function keys.
     */
    List<String> getKeys();

    /**
     * Return the raw code/definition for a given key if available.
     * This is shown/edited in the UI.
     */
    Optional<String> getCode(String key);

    /**
     * Evaluate the given function using the provided parameters.
     * Return the computed result as String.
     *
     * Note:
     * In your final system this should execute business logic
     * (e.g. generate OTP, wrap text, build regex).
     */
    String evaluate(String key, List<String> params);

    /**
     * Register or update a function definition.
     */
    void register(String key, String code);

    /**
     * Remove a function by key.
     */
    void remove(String key);

    /**
     * Persist all current functions.
     */
    void save();
}
