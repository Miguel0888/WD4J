package de.bund.zrb.expressions.builtins.tooling;

import de.bund.zrb.expressions.domain.ExpressionFunction;

import java.util.Collection;

/**
 * Implementiere dieses Interface in einem Tool, um dessen Methoden
 * als Built-in ExpressionFunctions anzubieten.
 */
public interface BuiltinTool {

    /**
     * Baue und liefere die Built-in Functions dieses Tools.
     * Jede Function ist vollständig beschreibbar (Name, Beschreibung, Parameternamen).
     */
    Collection<ExpressionFunction> builtinFunctions();

    /**
     * Convenience: Standard-Name unter dem die Functions gruppiert werden könnten (optional).
     * Kann für UI/IntelliSense-Grouping genutzt werden.
     */
    default String groupName() {
        return this.getClass().getSimpleName();
    }
}