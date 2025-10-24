package de.bund.zrb.auth;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.config.AuthDetectionConfig;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.tools.LoginTool;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * AutoAuthOrchestrator (vereinfachte Version)
 *
 * - speichert pro Top-Level-Context die "intendedUrl" bei NAVIGATION_STARTED
 * - lauscht auf RESPONSE_STARTED (nur document + top-level)
 * - wenn 302 → Login-URL erkannt, dann genau EINMAL Login auslösen (Debounce, pro Navigation)
 * - KEIN Zurücknavigieren (Server erledigt das selbst)
 * - KEIN network-intercept (verhindert pot. Hänger)
 */
public final class AutoAuthOrchestrator {

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

    private final AuthDetectionConfig authCfg;
    private final long loginDebounceMs;

    private final ConcurrentHashMap<String, Boolean> topLevelContexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ContextState> states = new ConcurrentHashMap<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoAuth-Worker");
        t.setDaemon(true);
        return t;
    });

    public AutoAuthOrchestrator(BrowserImpl browser, /* WDNetworkManager network (ung.) */ Object unusedNetwork, LoginTool loginTool) {
        this.browser = browser;
        this.wd = browser.getWebDriver();
        this.loginTool = loginTool;

        this.authCfg = AuthDetectionConfig.load();
        SettingsService s = SettingsService.getInstance();
        Long db = s.get("auth.debounceMs", Long.class);
        this.loginDebounceMs = (db == null ? 1500L : Math.max(0L, db));
    }

    /** Listener registrieren. */
    public void install() {
        if (!authCfg.enabled) return;
        subscribeContextCreatedDestroyed();
        subscribeNavigationStarted();
        subscribeResponseStarted();
    }

    // --------------------------------------------------------------------------------
    // Subscriptions
    // --------------------------------------------------------------------------------

    private void subscribeContextCreatedDestroyed() {
        wd.addEventListener(
                new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), null, null),
                (Consumer<Object>) ev -> {
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
                    worker.submit(() -> {
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
                    final WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
                    worker.submit(() -> {
                        try { handleResponseStarted(e.getParams()); } catch (Throwable ignored) {}
                    });
                }
        );
    }

    // --------------------------------------------------------------------------------
    // Core
    // --------------------------------------------------------------------------------

    private void handleResponseStarted(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
        if (p == null || p.getResponse() == null || p.getRequest() == null) return;

        final String ctx = safeVal(p.getContext());
        if (!isTopLevelCached(ctx)) return;

        // Nur Dokument-Navigationen (Subresources ignorieren → weniger Doppeltrigger)
        String dest = headerString(p.getRequest().getHeaders(), "sec-fetch-dest");
        if (!"document".equalsIgnoreCase(dest)) return;

        // navigationId korrekt lesen
        final String navId = (p.getNavigation() == null) ? null : p.getNavigation().value();
        if (navId == null || navId.isEmpty()) return;

        final int status = (int) p.getResponse().getStatus();
        if (!authCfg.redirectStatusCodes.contains(status) && status != 200) return;

        final String responseUrl = nullToEmpty(p.getResponse().getUrl());
        final String location    = headerString(p.getResponse().getHeaders(), "location");

        ContextState st = states.computeIfAbsent(ctx, k -> new ContextState());

        // Schleifenbremse
        long now = System.currentTimeMillis();
        if (now < st.suppressUntilTs) return;

        // Pro Navigation nur einmal die 302-Login-Erkennung verarbeiten
        if (Objects.equals(navId, st.lastHandledNavigationId)) return;

        // Benutzer ermitteln
        UserRegistry.User user = BrowserServiceImpl.getInstance().userForBrowsingContextId(ctx);
        if (user == null) return;

        final String loginUrl = user.getLoginPage();
        final boolean redirectToLogin = isRedirectToUserLogin(location, responseUrl, loginUrl);

        // 302 → Login-URL erkannt → genau EINMAL Login starten
        if (authCfg.redirectStatusCodes.contains(status) && redirectToLogin) {
            // Debounce
            if (st.loginAttempted && (now - st.lastLoginTs) < loginDebounceMs) return;

            st.loginAttempted = true;
            st.lastLoginTs = now;
            st.lastHandledNavigationId = navId;

            if (!st.loginInProgress) {
                st.loginInProgress = true;
                try {
                    // Erwartet: boolean-Return von loginTool.login(user)
                    // Falls dein Login noch ein Dialog-Stub ist, der weggeclickt wird:
                    // -> er kann false zurückgeben, aber das beeinflusst hier nichts mehr
                    try {
//                        loginTool.login(user);
                        JOptionPane.showMessageDialog(null, "LOGIN"); // TODO
                    } catch (Throwable t) {
                        // Vorläufiger Stub (falls Deine Signatur noch void ist o.ä.)
                        JOptionPane.showMessageDialog(null, "LOGIN FAILED");
                    }
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
    // Utils
    // --------------------------------------------------------------------------------

    private static boolean isRedirectToUserLogin(String locationHeader, String responseUrl, String configuredLoginUrl) {
        if (configuredLoginUrl == null || configuredLoginUrl.trim().isEmpty()) return false;

        String normLogin = normalizeForCompare(configuredLoginUrl);

        // 1) Location-Header (bei Redirects maßgeblich)
        if (locationHeader != null && !locationHeader.isEmpty()) {
            if (normalizeForCompare(locationHeader).equals(normLogin)) return true;
        }

        // 2) Fallback: Response-URL (manche Stacks tragen dort bereits die Ziel-Login-URL ein)
        if (responseUrl != null && !responseUrl.isEmpty()) {
            if (normalizeForCompare(responseUrl).equals(normLogin)) return true;
        }

        return false;
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
