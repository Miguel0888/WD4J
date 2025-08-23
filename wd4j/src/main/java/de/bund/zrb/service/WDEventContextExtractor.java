package de.bund.zrb.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Extract browsing/user context ids from BiDi event payloads.
 * Support both modern (enum + JsonObject) and legacy (String + Object) call sites.
 */
public final class WDEventContextExtractor {

    private WDEventContextExtractor() {
        // Prevent instantiation
    }

    // -------------------------------------------------------------------------------------------------------------
    // Public API (modern)
    // -------------------------------------------------------------------------------------------------------------

    public static String extractContextId(WDEventNames event, JsonObject params) {
        if (event == null || params == null) return null;

        // 1) Fast path: most BiDi events carry top-level "context"
        String ctx = getString(params, "context");
        if (notEmpty(ctx)) return ctx;

        // 2) Event-family specific carriers
        switch (event) {
            // Browsing context events
            case CONTEXT_CREATED:
            case CONTEXT_DESTROYED:
            case NAVIGATION_STARTED:
            case NAVIGATION_COMMITTED:
            case NAVIGATION_FAILED:
            case NAVIGATION_ABORTED:
            case FRAGMENT_NAVIGATED:
            case HISTORY_UPDATED:
            case DOM_CONTENT_LOADED:
            case LOAD:
            case DOWNLOAD_WILL_BEGIN:
            case USER_PROMPT_OPENED:
            case USER_PROMPT_CLOSED:
                // Already handled by top-level "context". Keep fallback for defensive coding.
                ctx = getString(params, "params", "context"); // tolerant wrapper payloads
                if (notEmpty(ctx)) return ctx;
                break;

            // Network events: BiDi carries top-level "context"
            case BEFORE_REQUEST_SENT:
            case RESPONSE_STARTED:
            case RESPONSE_COMPLETED:
            case FETCH_ERROR:
            case AUTH_REQUIRED:
                // Already handled by top-level "context"
                break;

            // Script events
            case REALM_CREATED:
            case REALM_DESTROYED:
                // script.realmCreated/Destroyed → realm.context
                ctx = getString(params, "realm", "context");
                if (notEmpty(ctx)) return ctx;
                break;

            case MESSAGE:
                // script.message → often carries source.context (fallbacks included)
                ctx = getString(params, "source", "context");
                if (notEmpty(ctx)) return ctx;
                ctx = getString(params, "realm", "context");
                if (notEmpty(ctx)) return ctx;
                break;

            // Log events
            case ENTRY_ADDED:
                // log.entryAdded → source.context
                ctx = getString(params, "source", "context");
                if (notEmpty(ctx)) return ctx;
                break;

            // Input events
            case FILE_DIALOG_OPENED:
                // input.fileDialogOpened usually carries top-level "context"
                break;

            default:
                break;
        }

        // 3) Generic fallbacks
        ctx = getString(params, "target", "context");
        if (notEmpty(ctx)) return ctx;

        ctx = getString(params, "params", "context");
        if (notEmpty(ctx)) return ctx;

        ctx = getString(params, "browsingContext");
        if (notEmpty(ctx)) return ctx;

        // 4) Last resort: limited deep search for context-like keys
        ctx = findFirstContextLikeKey(params, 3); // search up to depth 3
        return notEmpty(ctx) ? ctx : null;
    }

    public static String extractUserContextId(WDEventNames event, JsonObject params) {
        if (params == null) return null;

        // 1) Common carriers
        String uc = getString(params, "userContext");
        if (notEmpty(uc)) return uc;

        uc = getString(params, "context", "userContext");
        if (notEmpty(uc)) return uc;

        uc = getString(params, "params", "userContext");
        if (notEmpty(uc)) return uc;

        // 2) Deep search as last resort
        uc = findFirstKey(params, "userContext", 3);
        return notEmpty(uc) ? uc : null;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Public API (legacy compatibility)
    // -------------------------------------------------------------------------------------------------------------

    public static String extractContextId(String eventName, Object payload) {
        WDEventNames event = WDEventNames.fromName(eventName);
        JsonObject params = toJsonObject(payload);
        return extractContextId(event, params);
    }

    public static String extractUserContextId(String eventName, Object payload) {
        WDEventNames event = WDEventNames.fromName(eventName);
        JsonObject params = toJsonObject(payload);
        return extractUserContextId(event, params);
    }

    // -------------------------------------------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------------------------------------------

    /** Get string at a dotted path. Return null if any hop is missing or not a string/primitive. */
    private static String getString(JsonObject root, String k1) {
        if (root == null) return null;
        JsonElement el = root.get(k1);
        return primitiveToString(el);
    }

    private static String getString(JsonObject root, String k1, String k2) {
        JsonObject o1 = getObject(root, k1);
        if (o1 == null) return null;
        return getString(o1, k2);
    }

    private static String getString(JsonObject root, String k1, String k2, String k3) {
        JsonObject o1 = getObject(root, k1);
        if (o1 == null) return null;
        JsonObject o2 = getObject(o1, k2);
        if (o2 == null) return null;
        return getString(o2, k3);
    }

    /** Get nested object or null. */
    private static JsonObject getObject(JsonObject root, String key) {
        if (root == null) return null;
        JsonElement el = root.get(key);
        if (el != null && el.isJsonObject()) {
            return el.getAsJsonObject();
        }
        return null;
    }

    /** Convert a primitive JsonElement to string or null. */
    private static String primitiveToString(JsonElement el) {
        if (el == null) return null;
        if (el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) return p.getAsString();
            if (p.isNumber()) return String.valueOf(p.getAsNumber());
            if (p.isBoolean()) return String.valueOf(p.getAsBoolean());
        }
        return null;
    }

    /** Search first "context"-like key (context, Context, browsingContext, contextId) within limited depth. */
    private static String findFirstContextLikeKey(JsonObject root, int maxDepth) {
        if (root == null) return null;

        // Exact keys first
        String s = findFirstKey(root, "context", maxDepth);
        if (notEmpty(s)) return s;

        s = findFirstKey(root, "browsingContext", maxDepth);
        if (notEmpty(s)) return s;

        s = findFirstKey(root, "contextId", maxDepth);
        if (notEmpty(s)) return s;

        // Fuzzy: endsWith("Context")
        s = findFirstKeyEndingWith(root, "Context", maxDepth);
        return notEmpty(s) ? s : null;
    }

    /** Find first exact key and return its primitive-as-string value. */
    private static String findFirstKey(JsonObject obj, String key, int depth) {
        if (obj == null || depth < 0) return null;

        // Check current level
        if (obj.has(key)) {
            String v = primitiveToString(obj.get(key));
            if (notEmpty(v)) return v;
        }
        // Recurse objects and arrays
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            JsonElement el = e.getValue();
            String v = traverse(el, key, depth - 1);
            if (notEmpty(v)) return v;
        }
        return null;
    }

    /** Find first key that ends with suffix and return its primitive-as-string value. */
    private static String findFirstKeyEndingWith(JsonObject obj, String suffix, int depth) {
        if (obj == null || depth < 0) return null;

        // Scan current object
        Iterator<Map.Entry<String, JsonElement>> it = obj.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonElement> e = it.next();
            String k = e.getKey();
            if (k != null && k.endsWith(suffix)) {
                String v = primitiveToString(e.getValue());
                if (notEmpty(v)) return v;
            }
            String nested = traverse(e.getValue(), null, depth - 1, suffix);
            if (notEmpty(nested)) return nested;
        }
        return null;
    }

    /** Traverse element and search either exactKey or endsWith suffix. */
    private static String traverse(JsonElement el, String exactKey, int depth) {
        if (el == null || depth < 0) return null;
        if (el.isJsonObject()) {
            return findFirstKey(el.getAsJsonObject(), exactKey, depth);
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String v = traverse(arr.get(i), exactKey, depth);
                if (notEmpty(v)) return v;
            }
        }
        return null;
    }

    private static String traverse(JsonElement el, String exactKey, int depth, String endsWith) {
        if (el == null || depth < 0) return null;
        if (el.isJsonObject()) {
            return findFirstKeyEndingWith(el.getAsJsonObject(), endsWith, depth);
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String v = traverse(arr.get(i), exactKey, depth, endsWith);
                if (notEmpty(v)) return v;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Legacy payload conversion (Map/List → JsonObject)
    // -------------------------------------------------------------------------------------------------------------

    private static JsonObject toJsonObject(Object payload) {
        if (payload instanceof JsonObject) {
            return (JsonObject) payload;
        }
        JsonElement el = toJsonElement(payload);
        if (el != null && el.isJsonObject()) {
            return el.getAsJsonObject();
        }
        return new JsonObject(); // return empty object for safety
    }

    private static JsonElement toJsonElement(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof JsonElement) return (JsonElement) value;

        if (value instanceof String) return new JsonPrimitive((String) value);
        if (value instanceof Number) return new JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        if (value instanceof Character) return new JsonPrimitive(String.valueOf(value));

        if (value instanceof Map) {
            JsonObject o = new JsonObject();
            @SuppressWarnings("unchecked")
            Map<Object, Object> m = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> e : m.entrySet()) {
                String k = e.getKey() == null ? "null" : String.valueOf(e.getKey());
                o.add(k, toJsonElement(e.getValue()));
            }
            return o;
        }

        if (value instanceof Iterable) {
            JsonArray a = new JsonArray();
            @SuppressWarnings("unchecked")
            Iterable<Object> it = (Iterable<Object>) value;
            for (Object v : it) {
                a.add(toJsonElement(v));
            }
            return a;
        }

        if (value.getClass().isArray()) {
            JsonArray a = new JsonArray();
            // Handle Object[] only (primitive arrays uncommon for our payloads)
            Object[] arr = (Object[]) value;
            for (Object v : arr) {
                a.add(toJsonElement(v));
            }
            return a;
        }

        // Fallback: stringify unknown objects
        return new JsonPrimitive(String.valueOf(value));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Utils
    // -------------------------------------------------------------------------------------------------------------

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
