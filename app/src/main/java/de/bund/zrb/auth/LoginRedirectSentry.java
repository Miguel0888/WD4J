package de.bund.zrb.auth;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.config.LoginConfig;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.tools.LoginTool;
import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * LoginRedirectSentry (vereinfachte Auto-Login-Erkennung)
 *
 * - speichert pro Top-Level-Context die "intendedUrl" bei NAVIGATION_STARTED (nur für spätere Erweiterungen)
 * - lauscht auf RESPONSE_STARTED (nur document + top-level)
 * - wenn Redirect-Status + Ziel = Login-Seite → genau EINMAL Login auslösen (Debounce, pro Navigation)
 * - KEIN Zurücknavigieren (Server erledigt die Weiterleitung nach Login)
 * - KEIN network-intercept
 */
public final class LoginRedirectSentry {

    private static final class ContextState {
        volatile String intendedUrl;

        // Login-Steuerung
        volatile boolean loginInProgress;
        volatile boolean loginAttempted;
        volatile long    lastLoginTs;

        // Pro Navigation nur einmal behandeln
        volatile String lastHandledNavigationId;

        // Kurzzeit-Bremse gegen Doppelfeuer
        volatile long suppressUntilTs;
    }

    private final BrowserImpl browser;
    private final WebDriver wd;
    private final LoginTool loginTool;

    private final long loginDebounceMs;

    private final ConcurrentHashMap<String, Boolean> topLevelContexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ContextState> states = new ConcurrentHashMap<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LoginRedirectSentry-Worker");
        t.setDaemon(true);
        return t;
    });

    // Steuerung
    private volatile boolean enabled = false;
    private volatile boolean listenersRegistered = false;

    public LoginRedirectSentry(BrowserImpl browser, LoginTool loginTool) {
        this.browser = browser;
        this.wd = browser.getWebDriver();
        this.loginTool = loginTool;

        // globaler Debounce (optional via Settings); sonst 1500ms
        Long db = SettingsService.getInstance().get("auth.debounceMs", Long.class);
        this.loginDebounceMs = (db == null ? 1500L : Math.max(0L, db));
    }

    /** Listener registrieren und aktivieren (idempotent). */
    public synchronized void enable() {
        if (!listenersRegistered) {
            subscribeContextCreatedDestroyed();
            subscribeNavigationStarted();
            subscribeResponseStarted();
            listenersRegistered = true;
        }
        enabled = true;
    }

    /**
     * Deaktivieren (Soft-Disable).
     * Listener bleiben registriert, aber reagieren nicht mehr; interne Zustände werden geleert.
     * (Falls dein WebDriver removeEventListener unterstützt, könntest du hier zusätzlich deregistrieren.)
     */
    public synchronized void disable() {
        enabled = false;

        // Zustände aufräumen
        states.clear();
        topLevelContexts.clear();
    }

    /**
     * Optional: endgültig schließen. Danach nicht wiederverwendbar.
     * Nur aufrufen, wenn die Instanz nicht mehr benötigt wird.
     */
    public synchronized void dispose() {
        disable();
        worker.shutdownNow();
    }

    // --------------------------------------------------------------------------------
    // Subscriptions
    // --------------------------------------------------------------------------------

    private void subscribeContextCreatedDestroyed() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), null, null),
                (Consumer<Object>) ev -> {
                    if (!enabled) return;
                    if (!(ev instanceof WDBrowsingContextEvent.Created)) return;
                    WDBrowsingContextEvent.Created e = (WDBrowsingContextEvent.Created) ev;
                    String ctx = safeVal(e.getParams().getContext());
                    boolean isTop = (e.getParams().getParent() == null);
                    topLevelContexts.put(ctx, isTop);
                    states.computeIfAbsent(ctx, k -> new ContextState());
                }
        );

        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.CONTEXT_DESTROYED.getName(), null, null),
                (Consumer<Object>) ev -> {
                    if (!enabled) return;
                    if (!(ev instanceof WDBrowsingContextEvent.Destroyed)) return;
                    WDBrowsingContextEvent.Destroyed e = (WDBrowsingContextEvent.Destroyed) ev;
                    String ctx = safeVal(e.getParams().getContext());
                    topLevelContexts.remove(ctx);
                    states.remove(ctx);
                }
        );
    }

    private void subscribeNavigationStarted() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.NAVIGATION_STARTED.getName(), null, null),
                (Consumer<Object>) ev -> {
                    if (!(ev instanceof WDBrowsingContextEvent.NavigationStarted)) return;
                    final WDBrowsingContextEvent.NavigationStarted e = (WDBrowsingContextEvent.NavigationStarted) ev;
                    if (!enabled) return;
                    worker.submit(() -> {
                        if (!enabled) return;
                        String ctx = safeVal(e.getParams().getContext());
                        if (!isTopLevelCached(ctx)) return;

                        ContextState st = states.computeIfAbsent(ctx, k -> new ContextState());
                        st.intendedUrl = nullToEmpty(e.getParams().getUrl());
                        st.lastHandledNavigationId = null;

                        // kleine Bremse beim echten Navigationsstart zurücksetzen
                        if (System.currentTimeMillis() > st.suppressUntilTs) {
                            st.suppressUntilTs = 0L;
                        }
                    });
                }
        );
    }

    private void subscribeResponseStarted() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.RESPONSE_STARTED.getName(), null, null),
                (Consumer<Object>) ev -> {
                    if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return;
                    if (!enabled) return;
                    final WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
                    worker.submit(() -> {
                        if (!enabled) return;
                        try { handleResponseStarted(e.getParams()); } catch (Throwable ignored) {}
                    });
                }
        );
    }

    // --------------------------------------------------------------------------------
    // Core
    // --------------------------------------------------------------------------------

    private void handleResponseStarted(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        if (!enabled) return;
        if (p == null || p.getResponse() == null || p.getRequest() == null) return;

        final String ctx = safeVal(p.getContext());
        if (!isTopLevelCached(ctx)) return;

        // Nur Dokument-Navigationen (Subresources ignorieren → weniger Doppeltrigger)
        String dest = headerString(p.getRequest().getHeaders(), "sec-fetch-dest");
        if (!"document".equalsIgnoreCase(dest)) return;

        // navigationId korrekt lesen
        final String navId = (p.getNavigation() == null) ? null : p.getNavigation().value();
        if (navId == null || navId.isEmpty()) return;

        // Benutzer ermitteln & per-User LoginConfig lesen
        UserRegistry.User user = BrowserServiceImpl.getInstance().userForBrowsingContextId(ctx);
        if (user == null) return;
        final LoginConfig cfg = user.getLoginConfig();
        if (cfg == null || !cfg.isEnabled()) return;

        final int status = (int) p.getResponse().getStatus();
        // defensive Kopie, um ConcurrentModification zu vermeiden
        final Set<Integer> redirectStatuses =
                java.util.Collections.unmodifiableSet(new java.util.HashSet<Integer>(cfg.getRedirectStatusCodes()));
        if (!redirectStatuses.contains(status)) return; // nur echte Redirects behandeln

        final String responseUrl = nullToEmpty(p.getResponse().getUrl());
        final String location    = headerString(p.getResponse().getHeaders(), "location");

        ContextState st = states.computeIfAbsent(ctx, k -> new ContextState());

        // Schleifenbremse
        long now = System.currentTimeMillis();
        if (now < st.suppressUntilTs) return;

        // Pro Navigation nur einmal die Redirect-Login-Erkennung verarbeiten
        if (Objects.equals(navId, st.lastHandledNavigationId)) return;

        // Ziel-URL ist die Location (bei Redirect maßgeblich), Fallback responseUrl
        final String targetUrl = (location != null && !location.isEmpty()) ? location : responseUrl;

        if (isLoginTarget(targetUrl, cfg)) {
            // Debounce
            if (st.loginAttempted && (now - st.lastLoginTs) < loginDebounceMs) return;

            st.loginAttempted = true;
            st.lastLoginTs = now;
            st.lastHandledNavigationId = navId;

            if (!st.loginInProgress) {
                st.loginInProgress = true;
                try {
                    // loginTool.login(user);
                    JOptionPane.showMessageDialog(null, "LOGIN"); // TODO: Stub entfernen
                } catch (RuntimeException ex) {
                    // still fail → nächster Versuch erst nach Debounce
                } finally {
                    st.loginInProgress = false;
                }
            }

            // Nach Login-Versuch kurz dämpfen, um Doppelevents zu vermeiden
            st.suppressUntilTs = System.currentTimeMillis() + 1200L;
        }
    }

    // --------------------------------------------------------------------------------
    // Zielerkennung
    // --------------------------------------------------------------------------------

    private static boolean isLoginTarget(String targetUrl, LoginConfig cfg) {
        if (targetUrl == null || targetUrl.isEmpty()) return false;

        // 1) Exakte Login-URL
        String configuredLogin = cfg.getLoginPage();
        if (configuredLogin != null && !configuredLogin.trim().isEmpty()) {
            if (normalizeForCompare(targetUrl).equals(normalizeForCompare(configuredLogin))) {
                return true;
            }
        }

        // 2) Fallback: Prefix-Liste (z. B. /login, /signin)
        for (String prefix : cfg.getLoginUrlPrefixes()) {
            if (prefix == null || prefix.isEmpty()) continue;
            if (pathOf(targetUrl).startsWith(prefix.trim())) return true;
        }

        return false;
    }

    private static String pathOf(String url) {
        try {
            URI u = URI.create(url);
            String p = (u.getPath() == null || u.getPath().isEmpty()) ? "/" : u.getPath();
            // trailing slash entfernen außer root
            if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
            return p;
        } catch (Exception e) {
            // sehr simple Fallback-Extraktion
            String s = url;
            int scheme = s.indexOf("://");
            if (scheme >= 0) s = s.substring(scheme + 3);
            int slash = s.indexOf('/');
            s = (slash >= 0) ? s.substring(slash) : "/";
            if (s.length() > 1 && s.endsWith("/")) s = s.substring(0, s.length() - 1);
            return s;
        }
    }

    /**
     * Normalisiert URLs für Gleichheitsvergleiche:
     *  - lowercase host + scheme
     *  - Pfad normalized, trailing "/" entfernt (außer root)
     *  - Query & Fragment ignoriert
     */
    private static String normalizeForCompare(String url) {
        try {
            URI u = URI.create(url);
            String scheme = safeLower(u.getScheme());
            String host   = safeLower(u.getHost());
            int port      = u.getPort();
            String path   = (u.getPath() == null || u.getPath().isEmpty()) ? "/" : u.getPath();

            if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);

            StringBuilder b = new StringBuilder();
            if (scheme != null && !scheme.isEmpty()) b.append(scheme).append("://");
            if (host != null) b.append(host);
            if (port > 0 && port != defaultPort(scheme)) b.append(":").append(port);
            b.append(path);
            return b.toString();
        } catch (Exception e) {
            String s = url.trim();
            int q = s.indexOf('?');  if (q >= 0) s = s.substring(0, q);
            int h = s.indexOf('#');  if (h >= 0) s = s.substring(0, h);
            if (s.length() > 1 && s.endsWith("/")) s = s.substring(0, s.length() - 1);
            return s.toLowerCase(Locale.ROOT);
        }
    }

    private static String safeLower(String s) { return (s == null) ? null : s.toLowerCase(Locale.ROOT); }
    private static int defaultPort(String scheme) {
        if ("http".equalsIgnoreCase(scheme)) return 80;
        if ("https".equalsIgnoreCase(scheme)) return 443;
        return -1;
    }

    private boolean isTopLevelCached(String contextId) {
        if (contextId == null) return false;
        Boolean b = topLevelContexts.get(contextId);
        return (b == null) ? true : b.booleanValue(); // permissiv, wenn unbekannt
    }

    private static String safeVal(de.bund.zrb.type.browsingContext.WDBrowsingContext v) {
        return (v == null) ? null : v.value();
    }

    private static String nullToEmpty(String s) { return (s == null) ? "" : s; }

    private static String headerString(java.util.List<WDHeader> headers, String name) {
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
}
