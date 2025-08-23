package de.bund.zrb.service;

import java.util.*;

@SuppressWarnings("unchecked")
public final class WdEventContextExtractor {
    private WdEventContextExtractor() {}

    /** Extract browsing context id from a BiDi event payload (Map from your bridge). */
    public static String extractContextId(String eventName, Object payload) {
        Map<String, Object> root = asMap(payload);

        // 1) Fast-path: many BiDi events provide top-level "context"
        String ctx = str(root.get("context"));
        if (notEmpty(ctx)) return ctx;

        // 2) Common nested carriers by event family
        // log.entryAdded → source.context
        ctx = deep(root, "source", "context");
        if (notEmpty(ctx)) return ctx;

        // script.realmCreated/Destroyed → realm.context
        ctx = deep(root, "realm", "context");
        if (notEmpty(ctx)) return ctx;

        // network.* already handled via top-level "context" (but keep fallback)
        ctx = deep(root, "target", "context"); // some impls use target
        if (notEmpty(ctx)) return ctx;

        // 3) Generic fallbacks – tolerate wrapped payload shapes
        ctx = deep(root, "params", "context");
        if (notEmpty(ctx)) return ctx;

        ctx = str(root.get("browsingContext"));
        if (notEmpty(ctx)) return ctx;

        // 4) Last resort: scan shallow maps for "context"-like keys
        for (Map.Entry<String, Object> e : root.entrySet()) {
            if (e == null) continue;
            String k = e.getKey();
            if (k == null) continue;
            if (k.endsWith("context") || k.endsWith("Context") || "ctx".equals(k)) {
                String v = str(e.getValue());
                if (notEmpty(v)) return v;
            }
        }
        return null;
    }

    /** Extract user context id (when present) to maintain a BC→UC index. */
    public static String extractUserContextId(String eventName, Object payload) {
        Map<String, Object> root = asMap(payload);
        String uc = str(root.get("userContext"));
        if (notEmpty(uc)) return uc;
        uc = deep(root, "context", "userContext");
        if (notEmpty(uc)) return uc;
        uc = deep(root, "params", "userContext");
        return notEmpty(uc) ? uc : null;
    }

    private static String deep(Map<String, Object> m, String k1, String k2) {
        Map<String, Object> a = asMap(m.get(k1));
        return str(a.get(k2));
    }

    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : Collections.<String, Object>emptyMap();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean notEmpty(String s) { return s != null && !s.isEmpty(); }
}
