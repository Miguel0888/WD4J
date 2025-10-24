package de.bund.zrb.auth;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.command.request.parameters.network.AddInterceptParameters;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.manager.WDNetworkManager;
import de.bund.zrb.tools.LoginTool;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Express intent: Auto-login globally based on WD4J Network + BrowsingContext events. */
public final class AutoAuthOrchestrator {

    private static final class ContextState {
        volatile String intendedUrl;
        volatile boolean loginAttempted;
    }

    private final BrowserImpl browser;
    private final WDNetworkManager network;
    private final LoginTool loginTool;

    private final Map<String, ContextState> states = new ConcurrentHashMap<String, ContextState>();
    private volatile String interceptId;

    public AutoAuthOrchestrator(BrowserImpl browser, WDNetworkManager network, LoginTool loginTool) {
        this.browser = browser;
        this.network = network;
        this.loginTool = loginTool;
    }

    public void install() {
        subscribeNavigationStarted();
        installNetworkIntercept();
        subscribeResponseStarted();
    }

    // =================================================================================
    // Subscriptions
    // =================================================================================

    private void subscribeNavigationStarted() {
        WDSubscriptionRequest req = new WDSubscriptionRequest(
                WDEventNames.NAVIGATION_STARTED.getName(), null, null
        );

        Consumer<Object> handler = new Consumer<Object>() {
            public void accept(Object ev) {
                if (!(ev instanceof WDBrowsingContextEvent.NavigationStarted)) return;
                WDBrowsingContextEvent.NavigationStarted e = (WDBrowsingContextEvent.NavigationStarted) ev;

                String ctx = e.getParams().getContext().value();
                if (ctx == null || !isTopLevel(ctx)) return;

                ContextState st = states.get(ctx);
                if (st == null) {
                    st = new ContextState();
                    states.put(ctx, st);
                }
                st.intendedUrl = safe(e.getParams().getUrl());
                st.loginAttempted = false;
            }
        };

        browser.getWebDriver().addEventListener(req, handler);
    }

    private void installNetworkIntercept() {
        List<AddInterceptParameters.InterceptPhase> phases =
                java.util.Collections.singletonList(AddInterceptParameters.InterceptPhase.RESPONSE_STARTED);
        this.interceptId = network.addIntercept(phases).getIntercept().value();
    }

    private void subscribeResponseStarted() {
        WDSubscriptionRequest req = new WDSubscriptionRequest(
                WDEventNames.RESPONSE_STARTED.getName(), null, null
        );

        Consumer<Object> handler = new Consumer<Object>() {
            public void accept(Object ev) {
                if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return;

                WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
                WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p = e.getParams();
                if (p == null || p.getResponse() == null) return;

                // ❗ Context kommt aus getContextId() (WDBaseParameters)
                String ctx = (p.getContextId() != null) ? p.getContextId().value() : null;
                if (ctx == null || !isTopLevel(ctx)) return;

                int status = toInt(p.getResponse().getStatus());
                String respUrl = safe(p.getResponse().getUrl());
                String location = headerAsString(p.getResponse().getHeaders(), "location");

                de.bund.zrb.service.UserRegistry.User user = pickUser();
                if (user == null) return;

                ContextState st = states.get(ctx);
                if (st == null) {
                    st = new ContextState();
                    states.put(ctx, st);
                }
                if (st.loginAttempted) return;

                boolean looksLogin =
                        looksLikeLogin(location, user.getLoginPage()) ||
                                looksLikeLogin(respUrl, user.getLoginPage());

                if (isRedirect(status) && looksLogin) {
                    st.loginAttempted = true;
                    try {
                        loginTool.login(user);
                    } catch (RuntimeException ex) {
                        return; // keep non-fatal
                    }

                    String intent = st.intendedUrl;
                    String now = currentUrl(ctx);
                    if (intent != null && intent.length() > 0 && (now == null || !now.startsWith(intent))) {
                        navigate(ctx, intent);
                    }
                }
            }
        };

        browser.getWebDriver().addEventListener(req, handler);
    }

    // =================================================================================
    // Helpers
    // =================================================================================

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /** Decode header value (WDBytesValue → String). */
    private static String headerAsString(List<WDHeader> headers, String name) {
        if (headers == null || name == null) return "";
        String n = name.toLowerCase(Locale.ROOT);
        for (WDHeader h : headers) {
            if (h == null || h.getName() == null) continue;
            if (n.equals(h.getName().toLowerCase(Locale.ROOT))) {
                WDBytesValue v = h.getValue();
                return (v != null) ? safe(v.getValue()) : "";
            }
        }
        return "";
    }

    private de.bund.zrb.service.UserRegistry.User pickUser() {
        java.util.List<de.bund.zrb.service.UserRegistry.User> all =
                de.bund.zrb.service.UserRegistry.getInstance().getAll();
        return all.isEmpty() ? null : all.get(0);
    }

    private void navigate(String contextId, String url) {
        try {
            browser.getWebDriver().browsingContext().navigate(url, contextId);
        } catch (Throwable ignore) { /* keep non-fatal */ }
    }

    private String currentUrl(String contextId) {
        try {
            WDBrowsingContextResult.GetTreeResult tree =
                    browser.getWebDriver().browsingContext()
                            .getTree(new WDBrowsingContext(contextId), 0L);
            if (tree.getContexts().isEmpty()) return null;
            return tree.getContexts().iterator().next().getUrl();
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isTopLevel(String contextId) {
        try {
            WDBrowsingContextResult.GetTreeResult tree =
                    browser.getWebDriver().browsingContext()
                            .getTree(new WDBrowsingContext(contextId), 0L);
            if (tree.getContexts().isEmpty()) return true;
            return tree.getContexts().iterator().next().getParent() == null;
        } catch (Throwable t) {
            return true;
        }
    }

    private static int toInt(long v) {
        return (v > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (v < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) v);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /** Explicitly keep this signature to avoid collisions. */
    private static boolean looksLikeLogin(String currentOrLocation, String configuredLoginUrl) {
        if (currentOrLocation == null || currentOrLocation.isEmpty()) return false;
        if (configuredLoginUrl != null && configuredLoginUrl.length() > 0 && currentOrLocation.startsWith(configuredLoginUrl)) {
            return true;
        }
        String c = currentOrLocation.toLowerCase(Locale.ROOT);
        return c.contains("/login") || c.contains("/signin") || c.contains("redirect=");
    }
}
