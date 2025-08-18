package de.bund.zrb.meta;

import com.microsoft.playwright.BrowserContext;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.websocket.WDEvent;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDBaseParameters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Provide a simple in-memory event service for meta events (thread-safe). */
public final class MetaEventServiceImpl implements MetaEventService {

    private static final MetaEventServiceImpl INSTANCE = new MetaEventServiceImpl();

    // Legacy MetaEvent listeners (unfiltered)
    private final List<MetaEventListener> listeners = new CopyOnWriteArrayList<MetaEventListener>();

    // New typed listeners filtered by context (no extra global mapping)
    private final Map<String, List<WDEventListener>> listenersByUser = new ConcurrentHashMap<String, List<WDEventListener>>();
    private final Map<String, List<WDEventListener>> listenersByBrowsing = new ConcurrentHashMap<String, List<WDEventListener>>();

    private MetaEventServiceImpl() { }

    public static MetaEventServiceImpl getInstance() {
        return INSTANCE;
    }

    // ---------------- Legacy API ----------------

    public void addListener(MetaEventListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(MetaEventListener listener) {
        if (listener != null) listeners.remove(listener);
    }

    public void publish(MetaEvent event) {
        if (event == null) return;
        for (MetaEventListener l : listeners) {
            try { l.onMetaEvent(event); } catch (Throwable ignore) { /* Do not break others */ }
        }
    }

    // ---------------- New typed API ----------------

    @Override
    public void addListenerForUserContext(WDEventListener listener, String userContextId) {
        if (listener == null || userContextId == null) return;
        List<WDEventListener> list = listenersByUser.get(userContextId);
        if (list == null) {
            list = new CopyOnWriteArrayList<WDEventListener>();
            listenersByUser.put(userContextId, list);
        }
        if (!list.contains(listener)) list.add(listener);
    }

    @Override
    public void addListenerForBrowsingContext(WDEventListener listener, String browsingContextId) {
        if (listener == null || browsingContextId == null) return;
        List<WDEventListener> list = listenersByBrowsing.get(browsingContextId);
        if (list == null) {
            list = new CopyOnWriteArrayList<WDEventListener>();
            listenersByBrowsing.put(browsingContextId, list);
        }
        if (!list.contains(listener)) list.add(listener);
    }

    @Override
    public void removeListener(WDEventListener listener) {
        if (listener == null) return;
        removeFromMap(listenersByUser, listener);
        removeFromMap(listenersByBrowsing, listener);
    }

    private void removeFromMap(Map<String, List<WDEventListener>> m, WDEventListener l) {
        for (Map.Entry<String, List<WDEventListener>> e : m.entrySet()) {
            List<WDEventListener> list = e.getValue();
            list.remove(l);
            if (list.isEmpty()) {
                m.remove(e.getKey());
            }
        }
    }

    @Override
    public void publish(WDEvent<?> event) {
        if (event == null) return;

        // 1) Extract browsingContextId generically across modules
        String browsingContextId = tryExtractBrowsingContextId(event);

        // 2) Resolve userContextId by scanning existing contexts (no duplicate state)
        String userContextId = resolveUserContextIdByBrowsing(browsingContextId);

        // 3) Dispatch to registered listeners for those contexts
        dispatch(event, userContextId, browsingContextId);
    }

    // ---------------- Internal helpers ----------------

    /** Extract browsingContextId from event params across modules. */
    private String tryExtractBrowsingContextId(WDEvent<?> event) {
        Object params = event.getParams();
        if (params == null) return null;

        // a) Network DTOs: WDBaseParameters exposes getContextId() -> WDBrowsingContext
        if (params instanceof WDBaseParameters) {
            WDBrowsingContext ctx = ((WDBaseParameters) params).getContextId();
            String id = extractIdFromBrowsingContext(ctx);
            if (id != null) return id;
        }

        // b) Generic: try common getters on params (getContextId(), getContext(), getBrowsingContextId())
        String id = extractContextIdViaCommonGetters(params);
        if (id != null) return id;

        // c) Generic: look for fields named 'contextId' or 'context' and then extract id/value
        id = extractContextIdViaCommonFields(params);
        if (id != null) return id;

        // d) Fallback: some events (e.g., log.entryAdded) may carry a 'source' with an optional context
        id = tryDeepSourceContext(params);
        return id;
    }

    /** Extract id/value from a WDBrowsingContext-like object. */
    private String extractIdFromBrowsingContext(Object ctx) {
        if (ctx == null) return null;
        if (ctx instanceof WDBrowsingContext) {
            // Try getId(), then value()
            String id = tryCallNoArgString(ctx, "getId");
            if (id == null) id = tryCallNoArgString(ctx, "value");
            return id;
        }
        // Generic attempt if unknown type but exposes similar accessors
        String id = tryCallNoArgString(ctx, "getId");
        if (id == null) id = tryCallNoArgString(ctx, "value");
        return id;
    }

    /** Try standard getters on params for context object or id. */
    private String extractContextIdViaCommonGetters(Object params) {
        // Try getContextId(): often returns WDBrowsingContext
        Object ctx = tryCallNoArg(params, "getContextId");
        String id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        // Try getContext(): some DTOs may use this naming
        ctx = tryCallNoArg(params, "getContext");
        id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        // Try getBrowsingContextId(): if DTO returns String directly
        id = tryCallNoArgString(params, "getBrowsingContextId");
        return id;
    }

    /** Try common fields 'contextId' or 'context' on params. */
    private String extractContextIdViaCommonFields(Object params) {
        Object ctx = tryReadField(params, "contextId");
        String id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        ctx = tryReadField(params, "context");
        id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        // Sometimes DTO may store the id directly as String
        String direct = tryReadFieldAsString(params, "browsingContextId");
        if (direct != null) return direct;

        return null;
    }

    /** Handle cases like log.entryAdded where the context may be nested in a 'source' object. */
    private String tryDeepSourceContext(Object params) {
        Object source = tryCallNoArg(params, "getSource");
        if (source == null) source = tryReadField(params, "source");
        if (source == null) return null;

        // Try same extraction on 'source'
        Object ctx = tryCallNoArg(source, "getContextId");
        String id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        ctx = tryCallNoArg(source, "getContext");
        id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        id = tryCallNoArgString(source, "getBrowsingContextId");
        if (id != null) return id;

        ctx = tryReadField(source, "contextId");
        id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        ctx = tryReadField(source, "context");
        id = extractIdFromBrowsingContext(ctx);
        if (id != null) return id;

        id = tryReadFieldAsString(source, "browsingContextId");
        return id;
    }

    /** Resolve userContextId by scanning UserContextMappingService contexts; avoid duplicate storage. */
    private String resolveUserContextIdByBrowsing(String browsingContextId) {
        if (browsingContextId == null) return null;

        List<UserRegistry.User> users = UserRegistry.getInstance().getAllUsers();
        if (users == null || users.isEmpty()) return null;

        for (UserRegistry.User user : users) {
            BrowserContext bc = UserContextMappingService.getInstance().getContextForUser(user.getUsername());
            if (bc == null) continue;

            if (bc instanceof de.bund.zrb.UserContextImpl) {
                de.bund.zrb.UserContextImpl uc = (de.bund.zrb.UserContextImpl) bc;

                // Prefer a tiny helper in UserContextImpl if available: boolean hasPage(String id)
                if (userContextOwnsBrowsing(uc, browsingContextId)) {
                    try {
                        return uc.getUserContext().value();
                    } catch (Throwable ignore) {
                        // Continue scanning if userContext cannot be read
                    }
                }
            }
        }
        return null;
    }

    /** Check via public helper if present; otherwise reflect into pages map (minimal, no extra state). */
    private boolean userContextOwnsBrowsing(de.bund.zrb.UserContextImpl uc, String browsingContextId) {
        if (uc == null || browsingContextId == null) return false;

        // 1) Try dedicated helper if present: hasPage(String)
        try {
            Method m = uc.getClass().getMethod("hasPage", String.class);
            Object r = m.invoke(uc, browsingContextId);
            if (r instanceof Boolean) return ((Boolean) r).booleanValue();
        } catch (Throwable ignore) {
            // Fall through to minimal reflective peek
        }

        // 2) Reflect into 'pages.pages' Map<String, PageImpl> as seen in debugger
        try {
            Field fPages = uc.getClass().getDeclaredField("pages");
            fPages.setAccessible(true);
            Object pagesObj = fPages.get(uc);
            if (pagesObj == null) return false;

            Field fInner = pagesObj.getClass().getDeclaredField("pages");
            fInner.setAccessible(true);
            Object mapObj = fInner.get(pagesObj);
            if (mapObj instanceof Map) {
                Map<?,?> m = (Map<?,?>) mapObj;
                return m.containsKey(browsingContextId);
            }
        } catch (Throwable ignore) {
            // Return false if not accessible
        }
        return false;
    }

    /** Dispatch event to all listeners registered for the derived contexts. */
    private void dispatch(WDEvent<?> event, String userContextId, String browsingContextId) {
        // Deliver to user-scoped listeners
        if (userContextId != null) {
            List<WDEventListener> list = listenersByUser.get(userContextId);
            if (list != null) {
                for (WDEventListener l : list) {
                    try { l.onWDEvent(event); } catch (Throwable ignore) { }
                }
            }
        }
        // Deliver to browsing-scoped listeners
        if (browsingContextId != null) {
            List<WDEventListener> list = listenersByBrowsing.get(browsingContextId);
            if (list != null) {
                for (WDEventListener l : list) {
                    try { l.onWDEvent(event); } catch (Throwable ignore) { }
                }
            }
        }
    }

    // ---------------- Tiny reflection utilities ----------------

    /** Try to call a zero-arg method; return object. */
    private Object tryCallNoArg(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /** Try to call a zero-arg method; return String value. */
    private String tryCallNoArgString(Object target, String methodName) {
        Object v = tryCallNoArg(target, methodName);
        return v == null ? null : String.valueOf(v);
    }

    /** Try to read a declared field; return object. */
    private Object tryReadField(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /** Try to read a declared field; return String value. */
    private String tryReadFieldAsString(Object target, String fieldName) {
        Object v = tryReadField(target, fieldName);
        return v == null ? null : String.valueOf(v);
    }
}
