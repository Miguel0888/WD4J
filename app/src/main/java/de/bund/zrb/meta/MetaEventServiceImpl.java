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
import de.bund.zrb.type.script.WDSource;
import de.bund.zrb.websocket.WDEvent;

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

        // 1) Extract browsingContextId using strongly-typed DTOs only
        String browsingContextId = tryExtractBrowsingContextId(event);

        // 2) Resolve userContextId by scanning existing contexts (no duplicate state)
        String userContextId = resolveUserContextIdByBrowsing(browsingContextId);

        // 3) Dispatch to registered listeners for those contexts
        dispatch(event, userContextId, browsingContextId);
    }

    // ---------------- Context extraction (typed, no reflection) ----------------

    /** Extract browsingContextId from event params across known modules and DTOs. */
    private String tryExtractBrowsingContextId(WDEvent<?> event) {
        // --- Network module: params extend WDBaseParameters -> getContextId(): WDBrowsingContext
        if (event instanceof WDNetworkEvent.AuthRequired
                || event instanceof WDNetworkEvent.BeforeRequestSent
                || event instanceof WDNetworkEvent.FetchError
                || event instanceof WDNetworkEvent.ResponseStarted
                || event instanceof WDNetworkEvent.ResponseCompleted) {
            Object p = event.getParams();
            if (p instanceof WDBaseParameters) {
                WDBrowsingContext ctx = ((WDBaseParameters) p).getContextId();
                return getIdFromContext(ctx);
            }
            return null;
        }

        // --- BrowsingContext module ---
        if (event instanceof WDBrowsingContextEvent.Created) {
            WDInfo p = ((WDBrowsingContextEvent.Created) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.Destroyed) {
            WDInfo p = ((WDBrowsingContextEvent.Destroyed) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationStarted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationStarted) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.FragmentNavigated) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.FragmentNavigated) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.DomContentLoaded) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.DomContentLoaded) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.Load) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.Load) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.DownloadWillBegin) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.DownloadWillBegin) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationAborted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationAborted) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationFailed) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationFailed) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.NavigationCommitted) {
            WDNavigationInfo p = ((WDBrowsingContextEvent.NavigationCommitted) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
        }
        if (event instanceof WDBrowsingContextEvent.HistoryUpdated) {
            WDBrowsingContextEvent.HistoryUpdated.HistoryUpdatedParameters p =
                    ((WDBrowsingContextEvent.HistoryUpdated) event).getParams();
            return p != null ? getIdFromContext(p.getContext()) : null;
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

        // --- Log module: log.entryAdded -> WDLogEntry has concrete subtypes with getSource()
        if (event instanceof WDLogEvent.EntryAdded) {
            WDLogEvent.EntryAdded e = (WDLogEvent.EntryAdded) event;
            WDLogEntry entry = e.getParams();
            return getIdFromLogEntry(entry);
        }

        // --- Script module ---
        if (event instanceof WDScriptEvent.Message) {
            WDScriptEvent.Message.MessageParameters p = ((WDScriptEvent.Message) event).getParams();
            if (p == null) return null;
            WDSource source = p.getSource();
            return getIdFromSource(source);
        }
        if (event instanceof WDScriptEvent.RealmCreated) {
            // No reliable browsing context without an extra lookup; do not guess via realm
            return null;
        }
        if (event instanceof WDScriptEvent.RealmDestroyed) {
            // No browsing context in this event
            return null;
        }

        // Unknown or events without context
        return null;
    }

    // ---------------- DTO-specific helpers (typed, no reflection) ----------------

    /** Get the string ID from a WDBrowsingContext object. */
    private String getIdFromContext(WDBrowsingContext ctx) {
        return ctx != null ? ctx.value() : null;
    }

    /** Extract browsingContextId from a WDSource via its context. Avoid realm because it cannot be resolved locally. */
    private String getIdFromSource(WDSource source) {
        if (source == null) return null;
        WDBrowsingContext ctx = source.getContext();
        return getIdFromContext(ctx);
    }

    /** Extract browsingContextId from a WDLogEntry by casting to BaseWDLogEntry (which provides getSource()). */
    private String getIdFromLogEntry(WDLogEntry entry) {
        if (entry == null) return null;
        if (entry instanceof WDLogEntry.BaseWDLogEntry) {
            WDSource src = ((WDLogEntry.BaseWDLogEntry) entry).getSource();
            return getIdFromSource(src);
        }
        // If more implementations appear that do not extend BaseWDLogEntry, handle them explicitly here.
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

                // Use public helper; do not reflect internals
                if (uc.hasPage(browsingContextId)) {
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
