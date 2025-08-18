package de.bund.zrb.meta;

import com.microsoft.playwright.BrowserContext;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDLogEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDInfo;
import de.bund.zrb.type.browsingContext.WDNavigationInfo;
import de.bund.zrb.type.log.WDLogEntry;
import de.bund.zrb.type.network.WDBaseParameters;
import de.bund.zrb.type.script.WDRealm;
import de.bund.zrb.type.script.WDSource;
import de.bund.zrb.websocket.WDEvent;

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

        // 1) Extract browsingContextId targeted to existing DTOs
        String browsingContextId = tryExtractBrowsingContextId(event);

        // 2) Resolve userContextId by scanning existing contexts (no duplicate state)
        String userContextId = resolveUserContextIdByBrowsing(browsingContextId);

        // 3) Dispatch to registered listeners for those contexts
        dispatch(event, userContextId, browsingContextId);
    }

    // ---------------- Context extraction (targeted per module/DTO) ----------------

    /** Extract browsingContextId from event params across known modules and DTOs. */
    private String tryExtractBrowsingContextId(WDEvent<?> event) {
        // --- Network module: all params extend WDBaseParameters -> getContextId(): WDBrowsingContext
        if (event instanceof WDNetworkEvent.AuthRequired
                || event instanceof WDNetworkEvent.BeforeRequestSent
                || event instanceof WDNetworkEvent.FetchError
                || event instanceof WDNetworkEvent.ResponseCompleted
                || event instanceof WDNetworkEvent.ResponseStarted) {
            Object p = event.getParams();
            if (p instanceof WDBaseParameters) {
                WDBrowsingContext ctx = ((WDBaseParameters) p).getContextId();
                return idFromWDBrowsingContext(ctx);
            }
        }

        // --- BrowsingContext module ---
        if (event instanceof WDBrowsingContextEvent.Created) {
            WDInfo p = ((WDBrowsingContextEvent.Created) event).getParams();
            return idFromWDInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.Destroyed) {
            WDInfo p = ((WDBrowsingContextEvent.Destroyed) event).getParams();
            return idFromWDInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationStarted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationStarted) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.FragmentNavigated) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.FragmentNavigated) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.DomContentLoaded) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.DomContentLoaded) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.Load) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.Load) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.DownloadWillBegin) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.DownloadWillBegin) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationAborted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationAborted) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationFailed) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationFailed) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationCommitted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationCommitted) event).getParams();
            return idFromWDNavigationInfo(p);
        }
        if (event instanceof WDBrowsingContextEvent.HistoryUpdated) {
            WDBrowsingContextEvent.HistoryUpdated.HistoryUpdatedParameters p =
                    ((WDBrowsingContextEvent.HistoryUpdated) event).getParams();
            if (p != null) return idFromWDBrowsingContext(p.getContext());
            return null;
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptOpened) {
            WDBrowsingContextEvent.UserPromptOpened.UserPromptOpenedParameters p =
                    ((WDBrowsingContextEvent.UserPromptOpened) event).getParams();
            return p != null ? p.getContext() : null; // already a String id
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptClosed) {
            WDBrowsingContextEvent.UserPromptClosed.UserPromptClosedParameters p =
                    ((WDBrowsingContextEvent.UserPromptClosed) event).getParams();
            return p != null ? p.getContext() : null; // already a String id
        }

        // --- Log module: log.entryAdded -> WDLogEntry carries WDSource; WDSource may carry context or realm->context
        if (event instanceof WDLogEvent.EntryAdded) {
            WDLogEntry entry = ((WDLogEvent.EntryAdded) event).getParams();
            return idFromWDLogEntry(entry);
        }

        // --- Script module ---
        if (event instanceof WDScriptEvent.Message) {
            WDScriptEvent.Message.MessageParameters p = ((WDScriptEvent.Message) event).getParams();
            if (p != null) return idFromWDSource(p.getSource());
            return null;
        }
        if (event instanceof WDScriptEvent.RealmCreated) {
            WDScriptEvent.RealmCreated.RealmCreatedParameters p = ((WDScriptEvent.RealmCreated) event).getParams();
            if (p != null) return idFromWDRealm(p.getRealm());
            return null;
        }
        if (event instanceof WDScriptEvent.RealmDestroyed) {
            // RealmDestroyed has only realm id; context not available
            return null;
        }

        // Unknown or events without context
        return null;
    }

    // ---------------- DTO-specific helpers ----------------

    /** Extract id from WDBrowsingContext (value() or getId()). */
    private String idFromWDBrowsingContext(WDBrowsingContext ctx) {
        if (ctx == null) return null;
        // Prefer value() if it exists in your DTOs (as shown for WDUserContext)
        try {
            Method m = ctx.getClass().getMethod("value");
            Object v = m.invoke(ctx);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable ignore) {
            try {
                Method m = ctx.getClass().getMethod("getId");
                Object v = m.invoke(ctx);
                return v == null ? null : String.valueOf(v);
            } catch (Throwable ignoreToo) {
                return null;
            }
        }
    }

    /** Extract id from WDInfo (browsingContext info) DTO. */
    private String idFromWDInfo(WDInfo info) {
        if (info == null) return null;
        // Expect a context accessor; try common names until DTO is finalized
        try {
            Method m = info.getClass().getMethod("getContext");
            Object ctx = m.invoke(info);
            if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
            if (ctx != null) return String.valueOf(ctx);
        } catch (Throwable ignore) {
            // Try getContextId()
            try {
                Method m = info.getClass().getMethod("getContextId");
                Object ctx = m.invoke(info);
                if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
                if (ctx != null) return String.valueOf(ctx);
            } catch (Throwable ignoreToo) {
                // Try field 'context'
                try {
                    Field f = info.getClass().getDeclaredField("context");
                    f.setAccessible(true);
                    Object ctx = f.get(info);
                    if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
                    if (ctx != null) return String.valueOf(ctx);
                } catch (Throwable ignoreThree) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Extract id from WDNavigationInfo DTO. */
    private String idFromWDNavigationInfo(WDNavigationInfo nav) {
        if (nav == null) return null;
        // Try getContext()
        try {
            Method m = nav.getClass().getMethod("getContext");
            Object ctx = m.invoke(nav);
            if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
            if (ctx != null) return String.valueOf(ctx);
        } catch (Throwable ignore) {
            // Try getContextId()
            try {
                Method m = nav.getClass().getMethod("getContextId");
                Object ctx = m.invoke(nav);
                if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
                if (ctx != null) return String.valueOf(ctx);
            } catch (Throwable ignoreToo) {
                // Try field 'context'
                try {
                    Field f = nav.getClass().getDeclaredField("context");
                    f.setAccessible(true);
                    Object ctx = f.get(nav);
                    if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
                    if (ctx != null) return String.valueOf(ctx);
                } catch (Throwable ignoreThree) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Extract id from WDLogEntry via its WDSource (context or realm->context). */
    private String idFromWDLogEntry(WDLogEntry entry) {
        if (entry == null) return null;
        try {
            Method m = entry.getClass().getMethod("getSource");
            Object src = m.invoke(entry);
            if (src instanceof WDSource) {
                return idFromWDSource((WDSource) src);
            }
            // If DTO differs, try field 'source'
            if (src == null) {
                try {
                    Field f = entry.getClass().getDeclaredField("source");
                    f.setAccessible(true);
                    Object s = f.get(entry);
                    if (s instanceof WDSource) return idFromWDSource((WDSource) s);
                } catch (Throwable ignore) { /* fall through */ }
            }
        } catch (Throwable ignore) {
            // fall through
        }
        return null;
    }

    /** Extract id from WDSource: prefer direct context, otherwise realm->context. */
    private String idFromWDSource(WDSource source) {
        if (source == null) return null;
        // Try direct context accessor
        try {
            Method m = source.getClass().getMethod("getContext");
            Object ctx = m.invoke(source);
            if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
            if (ctx instanceof String) return (String) ctx;
            if (ctx != null) return String.valueOf(ctx);
        } catch (Throwable ignore) {
            // continue
        }
        // Try realm and read its context
        try {
            Method m = source.getClass().getMethod("getRealm");
            Object realm = m.invoke(source);
            if (realm instanceof WDRealm) {
                return idFromWDRealm((WDRealm) realm);
            }
        } catch (Throwable ignore) {
            // continue
        }
        return null;
    }

    /** Extract id from WDRealm (realm may have optional browsing context according to spec). */
    private String idFromWDRealm(WDRealm realm) {
        if (realm == null) return null;
        try {
            Method m = realm.getClass().getMethod("getContext");
            Object ctx = m.invoke(realm);
            if (ctx instanceof WDBrowsingContext) return idFromWDBrowsingContext((WDBrowsingContext) ctx);
            if (ctx instanceof String) return (String) ctx;
            if (ctx != null) return String.valueOf(ctx);
        } catch (Throwable ignore) {
            // continue
        }
        return null;
    }

    // ---------------- Resolve user context by scanning existing contexts ----------------

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

    // ---------------- Dispatch ----------------

    /** Deliver event to all listeners registered for the derived contexts. */
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
}
