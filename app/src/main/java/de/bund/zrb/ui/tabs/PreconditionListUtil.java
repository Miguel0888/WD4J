package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.Precondtion;

/** Utility helpers to deal with precondition references inside Given lists. */
public final class PreconditionListUtil {

    /** Conventional type value used to mark a Given entry as precondition reference. */
    public static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private PreconditionListUtil() {
        // utility
    }

    /**
     * Extract the referenced precondition id from the Given entry value (key=value&...).
     * Returns an empty string when no id could be resolved.
     */
    public static String extractPreconditionId(Precondtion given) {
        if (given == null) {
            return "";
        }
        String value = given.getValue();
        if (value == null || value.isEmpty()) {
            return "";
        }
        String[] parts = value.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) {
                return kv[1];
            }
        }
        return "";
    }
}

