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

        // 1) Extract browsingContextId targeted to existing DTOs without reflection
        String browsingContextId = tryExtractBrowsingContextId(event);

        // 2) Resolve userContextId by scanning existing contexts (no duplicate state)
        String userContextId = resolveUserContextIdByBrowsing(browsingContextId);

        // 3) Dispatch to registered listeners for those contexts
        dispatch(event, userContextId, browsingContextId);
    }

    // ---------------- Context extraction (targeted per module/DTO) ----------------

    /**
     * Extract browsingContextId from event parameters across known modules and DTOs,
     * using direct getters instead of reflection.
     */
    private String tryExtractBrowsingContextId(WDEvent<?> event) {
        // --- Network module events: all params extend WDBaseParameters with getContextId() -> WDBrowsingContext
        if (event instanceof WDNetworkEvent.AuthRequired
                || event instanceof WDNetworkEvent.BeforeRequestSent
                || event instanceof WDNetworkEvent.FetchError
                || event instanceof WDNetworkEvent.ResponseStarted
                || event instanceof WDNetworkEvent.ResponseCompleted) {
            // Cast to base network event type and get parameters
            WDBaseParameters params = (WDBaseParameters) event.getParams();
            if (params != null) {
                WDBrowsingContext ctx = params.getContextId();
                return getIdFromContext(ctx);  // directly get the ID from WDBrowsingContext
            }
            return null;
        }

        // --- BrowsingContext module events ---
        if (event instanceof WDBrowsingContextEvent.Created) {
            WDInfo params = ((WDBrowsingContextEvent.Created) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.Destroyed) {
            WDInfo params = ((WDBrowsingContextEvent.Destroyed) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationStarted) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.NavigationStarted) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.FragmentNavigated) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.FragmentNavigated) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.DomContentLoaded) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.DomContentLoaded) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.Load) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.Load) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.DownloadWillBegin) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.DownloadWillBegin) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationAborted) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.NavigationAborted) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationFailed) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.NavigationFailed) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationCommitted) {
            WDNavigationInfo params = ((WDBrowsingContextEvent.NavigationCommitted) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.HistoryUpdated) {
            // HistoryUpdated has its own parameter type with a context field
            WDBrowsingContextEvent.HistoryUpdated.HistoryUpdatedParameters params =
                    ((WDBrowsingContextEvent.HistoryUpdated) event).getParams();
            return (params != null) ? getIdFromContext(params.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptOpened) {
            // These events carry the context ID directly as a String in their params
            WDBrowsingContextEvent.UserPromptOpened.UserPromptOpenedParameters params =
                    ((WDBrowsingContextEvent.UserPromptOpened) event).getParams();
            return (params != null) ? params.getContext() : null;  // already a String ID
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptClosed) {
            WDBrowsingContextEvent.UserPromptClosed.UserPromptClosedParameters params =
                    ((WDBrowsingContextEvent.UserPromptClosed) event).getParams();
            return (params != null) ? params.getContext() : null;  // already a String ID
        }

        // --- Log module events: WDLogEntry contains a WDSource, which may have context or realm
        if (event instanceof WDLogEvent.EntryAdded) {
            WDLogEntry entry = ((WDLogEvent.EntryAdded) event).getParams();
            if (entry != null) {
                WDSource source = entry.getSource();
                return getIdFromSource(source);  // extract context ID from the WDSource
            }
            return null;
        }

        // --- Script module events ---
        if (event instanceof WDScriptEvent.Message) {
            WDScriptEvent.Message.MessageParameters params = ((WDScriptEvent.Message) event).getParams();
            if (params != null) {
                WDSource source = params.getSource();
                return getIdFromSource(source);
            }
            return null;
        }
        if (event instanceof WDScriptEvent.RealmCreated) {
            WDScriptEvent.RealmCreated.RealmCreatedParameters params = ((WDScriptEvent.RealmCreated) event).getParams();
            if (params != null) {
                WDRealm realm = params.getRealm();
                return getIdFromRealm(realm);
            }
            return null;
        }
        if (event instanceof WDScriptEvent.RealmDestroyed) {
            // RealmDestroyed events do not include a browsing context
            return null;
        }

        // Unknown or unsupported event type (no context to extract)
        return null;
    }

    // --- DTO-specific helper methods (no reflection) ---

    /** Get the string ID from a WDBrowsingContext object. */
    private String getIdFromContext(WDBrowsingContext ctx) {
        return (ctx != null) ? ctx.value() : null;
    }

    /** Extract browsingContextId from a WDRealm (if present). */
    private String getIdFromRealm(WDRealm realm) {
        if (realm == null) return null;
        String realmId = realm.value();
        // ToDo: Get Context from RealmId:
        WDBrowsingContext ctx = null;

        return (ctx != null) ? ctx.value() : null;
    }

    /** Extract browsingContextId from a WDSource via its context or realm. */
    private String getIdFromSource(WDSource source) {
        if (source == null) return null;
        // Prefer direct context if available
        WDBrowsingContext ctx = source.getContext();
        if (ctx != null) {
            return ctx.value();
        }
        // Fallback to realm's context if direct context is not set
        String realm = source.getRealm();
        return realm;
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
