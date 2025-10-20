package de.bund.zrb.service;

import de.bund.zrb.model.GivenCondition;

import java.util.Map;

/**
 * Provide helpers to create and interpret precondition references inside GivenCondition.
 * Keep the contract stable for UI and playback.
 */
public final class PreconditionRef {

    public static final String TYPE = "precond-ref";
    public static final String PARAM_REF_ID = "refId";

    private PreconditionRef() {}

    /** Create a GivenCondition that references a precondition by id. */
    public static GivenCondition ref(String preconditionId) {
        if (preconditionId == null || preconditionId.trim().isEmpty()) {
            throw new IllegalArgumentException("preconditionId must not be empty");
        }
        GivenCondition g = new GivenCondition();
        g.setType(TYPE);
        // Use the existing value/parameterMap convention in your project:
        // store as key=value so getParameterMap() can read it
        g.setValue(PARAM_REF_ID + "=" + preconditionId);
        return g;
    }

    /** Extract the referenced precondition id from a GivenCondition; return null if not a ref. */
    public static String extractRefId(GivenCondition g) {
        if (g == null || !TYPE.equals(g.getType())) return null;
        Map<String, Object> map = g.getParameterMap();
        if (map == null) return null;
        Object v = map.get(PARAM_REF_ID);
        return (v instanceof String) ? (String) v : null;
    }
}
