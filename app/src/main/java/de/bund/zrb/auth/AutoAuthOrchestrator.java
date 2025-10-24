package de.bund.zrb.auth;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.command.request.parameters.network.AddInterceptParameters;
import de.bund.zrb.config.AuthDetectionConfig;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.manager.WDNetworkManager;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.tools.LoginTool;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Transparentes Auto-Login über WebDriver BiDi Network-Intercepts.
 *
 * Strategie:
 *  - Intended URL je Top-Level-Tab auf NAVIGATION_STARTED mitschneiden.
 *  - Nur auf RESPONSE_STARTED hören (optional BEFORE_REQUEST_SENT/AUTH_REQUIRED kann später ergänzt werden).
 *  - Erkennung: (konfigurierbare) Redirect-Status + (optionale) Login-URL-Präfixe + (optional) Set-Cookie mit Session-Cookie.
 *  - Bei Treffer: passenden Benutzer aus BrowsingContext ableiten und Login off-thread durchführen.
 *  - Optional (config): anschließend zur ursprünglichen Intended-URL zurück navigieren.
 *  - Intercepts werden NICHT blockierend genutzt; bei isBlocked → continueResponse(requestId).
 */
public final class AutoAuthOrchestrator {

    private static final class ContextState {
        volatile String intendedUrl;
        volatile boolean loginAttempted;
        volatile long lastLoginTs;
    }

    private final BrowserImpl browser;
    private final WebDriver wd;
    private final WDNetworkManager network;
    private final LoginTool loginTool;

    private final AuthDetectionConfig authCfg;
    private final long loginDebounceMs;
    private final boolean returnToIntent;

    // Top-Level-Cache und State je BrowsingContext
    private final ConcurrentMap<String, Boolean> topLevelContexts = new ConcurrentHashMap<String, Boolean>();
    private final ConcurrentMap<String, ContextState> states      = new ConcurrentHashMap<String, ContextState>();

    // Worker für Heavy-Lifting (kein Blockieren des Dispatchers/EDT)
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "AutoAuth-Worker");
            t.setDaemon(true);
            return t;
        }
    });

    private volatile String interceptId;

    public AutoAuthOrchestrator(BrowserImpl browser, WDNetworkManager network, LoginTool loginTool) {
        this.browser = browser;
        this.network = network;
        this.loginTool = loginTool;
        this.wd = browser.getWebDriver();

        // Konfiguration laden
        this.authCfg = AuthDetectionConfig.load();
        SettingsService s = SettingsService.getInstance();
        Long db = s.get("auth.debounceMs", Long.class);
        Boolean rti = s.get("auth.returnToIntent", Boolean.class);
        this.loginDebounceMs = (db == null ? 1500L : Math.max(0L, db));
        this.returnToIntent  = (rti == null) || rti.booleanValue();
    }

    /** Registriert alle Listener und Intercepts. */
    public void install() {
        if (!authCfg.enabled) return;

        subscribeContextCreatedDestroyed(); // Top-Level bestimmen
        subscribeNavigationStarted();       // Intended-URL merken
        installNetworkIntercept();          // Intercept RESPONSE_STARTED (non-blocking)
        subscribeResponseStarted();         // Login-Erkennung
    }

    // --------------------------------------------------------------------------------
    // Subscriptions
    // --------------------------------------------------------------------------------

    private void subscribeContextCreatedDestroyed() {
        // CREATED → Top-Level markieren
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), null, null),
                new Consumer<Object>() { public void accept(Object ev) {
                    if (!(ev instanceof WDBrowsingContextEvent.Created)) return;
                    WDBrowsingContextEvent.Created e = (WDBrowsingContextEvent.Created) ev;
                    de.bund.zrb.type.browsingContext.WDInfo info = e.getParams();
                    String ctx = safeVal(info.getContext());
                    boolean isTop = (info.getParent() == null);
                    topLevelContexts.put(ctx, Boolean.valueOf(isTop));
                    states.computeIfAbsent(ctx, k -> new ContextState());
                }}
        );

        // DESTROYED → Cleanup
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.CONTEXT_DESTROYED.getName(), null, null),
                new Consumer<Object>() { public void accept(Object ev) {
                    if (!(ev instanceof WDBrowsingContextEvent.Destroyed)) return;
                    WDBrowsingContextEvent.Destroyed e = (WDBrowsingContextEvent.Destroyed) ev;
                    String ctx = safeVal(e.getParams().getContext());
                    topLevelContexts.remove(ctx);
                    states.remove(ctx);
                }}
        );
    }

    private void subscribeNavigationStarted() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.NAVIGATION_STARTED.getName(), null, null),
                new Consumer<Object>() { public void accept(Object ev) {
                    if (!(ev instanceof WDBrowsingContextEvent.NavigationStarted)) return;
                    final WDBrowsingContextEvent.NavigationStarted e = (WDBrowsingContextEvent.NavigationStarted) ev;
                    worker.submit(new Runnable() { public void run() {
                        String ctx = safeVal(e.getParams().getContext());
                        if (!isTopLevelCached(ctx)) return;
                        ContextState st = states.computeIfAbsent(ctx, k -> new ContextState());
                        st.intendedUrl = nullToEmpty(e.getParams().getUrl());
                        st.loginAttempted = false;
                    }});
                }}
        );
    }

    private void installNetworkIntercept() {
        // Wir brauchen hier nur RESPONSE_STARTED, optional kannst du BEFORE_REQUEST_SENT/AUTH_REQUIRED mit anhängen.
        List<AddInterceptParameters.InterceptPhase> phases =
                java.util.Collections.singletonList(AddInterceptParameters.InterceptPhase.RESPONSE_STARTED);
        this.interceptId = network.addIntercept(phases).getIntercept().value();
    }

    private void subscribeResponseStarted() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.RESPONSE_STARTED.getName(), null, null),
                new Consumer<Object>() { public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return;
                    final WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
                    worker.submit(new Runnable() { public void run() {
                        try { handleResponseStarted(e.getParams()); } catch (Throwable t) { /* swallow */ }
                    }});
                }}
        );
    }

    // --------------------------------------------------------------------------------
    // Core-Logik
    // --------------------------------------------------------------------------------

    private void handleResponseStarted(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        if (p == null || p.getResponse() == null) return;

        // Sofort freigeben, falls geblockt (non-blocking Design)
        continueIfBlocked(p);

        String ctx = safeVal(p.getContext());
        if (!isTopLevelCached(ctx)) return;

        int status = (int) p.getResponse().getStatus();
        if (!authCfg.redirectStatusCodes.contains(status)) return;

        String respUrl = nullToEmpty(p.getResponse().getUrl());
        String location = headerString(p.getResponse().getHeaders(), "location");
        boolean looksLogin = matchesLoginUrl(respUrl) || matchesLoginUrl(location) || setCookieContainsSession(p);

        if (!looksLogin) return;

        ContextState st = states.computeIfAbsent(ctx, k -> new ContextState());

        long now = System.currentTimeMillis();
        if (st.loginAttempted && (now - st.lastLoginTs) < loginDebounceMs) {
            // innerhalb Debounce → nichts tun
            return;
        }

        st.loginAttempted = true;
        st.lastLoginTs = now;

        // richtigen User aus Kontext herleiten
        UserRegistry.User user = BrowserServiceImpl.getInstance().userForBrowsingContextId(ctx);
        if (user == null) return;

        // Login off-thread durchführen (UI/EDT bleibt frei)
        try {
            loginTool.login(user);
        } catch (RuntimeException ex) {
            // Login schlug fehl → weiterer Versuch erst nach Debounce
            return;
        }

        // Optional: zurück zur Intended-URL springen
        if (returnToIntent) {
            String intent = emptyToNull(st.intendedUrl);
            if (intent != null) {
                String nowUrl = currentUrlSafe(ctx);
                if (nowUrl == null || !nowUrl.startsWith(intent)) {
                    try {
                        wd.browsingContext().navigate(intent, ctx);
                    } catch (Throwable ignore) {}
                }
            }
        }
    }

    private void continueIfBlocked(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        if (!p.isBlocked()) return;
        try {
            String reqId = requestIdOf(p);
            if (reqId != null) network.continueResponse(reqId);
        } catch (Throwable ignore) {}
    }

    // --------------------------------------------------------------------------------
    // Helper
    // --------------------------------------------------------------------------------

    private boolean isTopLevelCached(String contextId) {
        if (contextId == null) return false;
        Boolean b = topLevelContexts.get(contextId);
        return b == null ? true : b.booleanValue(); // permissiv, wenn unbekannt
    }

    private static String requestIdOf(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        try {
            return p.getRequest().getRequest().value();
        } catch (Throwable t) { return null; }
    }

    private boolean matchesLoginUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String u = url.toLowerCase(Locale.ROOT);

        // Konfigurierte Präfixe
        for (String pref : authCfg.loginUrlPrefixes) {
            if (pref == null || pref.trim().isEmpty()) continue;
            String pp = pref.toLowerCase(Locale.ROOT);
            if (u.startsWith(pp)) return true;

            // häufige Fälle: auch absolute Präfixe erlauben
            // z.B. https://host/app/login  vs.  /app/login
            int idx = u.indexOf('/', "https://".length());
            if (idx > 0) {
                String pathOnly = u.substring(idx);
                if (pathOnly.startsWith(pp)) return true;
            }
        }

        // Fallback Heuristik
        return u.contains("/login") || u.contains("/signin") || u.contains("redirect=");
    }

    private boolean setCookieContainsSession(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        String cookieHdr = headerString(p.getResponse().getHeaders(), "set-cookie");
        if (cookieHdr.isEmpty()) return false;
        String name = authCfg.sessionCookieName.toLowerCase(Locale.ROOT);
        return cookieHdr.toLowerCase(Locale.ROOT).contains(name + "=");
    }

    private static String headerString(List<WDHeader> headers, String name) {
        if (headers == null || name == null) return "";
        String n = name.toLowerCase(Locale.ROOT);
        for (WDHeader h : headers) {
            if (h == null || h.getName() == null) continue;
            if (n.equals(h.getName().toLowerCase(Locale.ROOT))) {
                WDBytesValue v = h.getValue();
                return (v != null) ? nullToEmpty(v.getValue()) : "";
            }
        }
        return "";
    }

    private String currentUrlSafe(String ctx) {
        try {
            de.bund.zrb.command.response.WDBrowsingContextResult.GetTreeResult tree =
                    wd.browsingContext().getTree(new WDBrowsingContext(ctx), 0L);
            if (tree.getContexts().isEmpty()) return null;
            return tree.getContexts().iterator().next().getUrl();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String safeVal(de.bund.zrb.type.browsingContext.WDBrowsingContext v) {
        return v == null ? null : v.value();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isEmpty()) ? null : s; }
}
