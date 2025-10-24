package de.bund.zrb.auth;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.tools.LoginTool;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Express intent: Auto-login globally based on BrowsingContext events (top-level only). */
public final class AutoAuthOrchestrator {

    /** Per-context state to remember the intended target and prevent loops. */
    private static final class ContextState {
        volatile String intendedUrl;
        volatile boolean loginAttempted;
    }

    private final Map<String, ContextState> states = new ConcurrentHashMap<String, ContextState>();

    private final BrowserImpl browser;
    private final LoginTool loginTool;

    public AutoAuthOrchestrator(BrowserImpl browser, LoginTool loginTool) {
        this.browser = browser;
        this.loginTool = loginTool;
    }

    /** Install once during browser startup. */
    public void install() {
        subscribeNavigationStarted(); // remember intent
        subscribeLoad();              // detect login page and act
    }

    // =================================================================================
    // Subscriptions
    // =================================================================================

    private void subscribeNavigationStarted() {
        // Listen globally (no specific context): WDSubscriptionRequest(eventName, contextId, realmId)
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

    private void subscribeLoad() {
        WDSubscriptionRequest req = new WDSubscriptionRequest(
                WDEventNames.LOAD.getName(), null, null
        );

        Consumer<Object> handler = new Consumer<Object>() {
            public void accept(Object ev) {
                if (!(ev instanceof WDBrowsingContextEvent.Load)) return;

                WDBrowsingContextEvent.Load e = (WDBrowsingContextEvent.Load) ev;
                String ctx = e.getParams().getContext().value();
                if (ctx == null || !isTopLevel(ctx)) return;

                String current = safe(e.getParams().getUrl());
                UserRegistry.User user = pickUserForAutoLogin();
                if (user == null) return;

                ContextState st = states.get(ctx);
                if (st == null) {
                    st = new ContextState();
                    states.put(ctx, st);
                }

                // Already attempted for this intent?
                if (st.loginAttempted) return;

                // Detect login page and act once
                if (looksLikeLogin(current, user.getLoginPage())) {
                    st.loginAttempted = true;

                    // 1) Do login once
                    try {
                        loginTool.login(user);
                    } catch (RuntimeException ex) {
                        // Keep non-fatal
                        return;
                    }

                    // 2) Navigate back to the intended target if needed
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

    /** Return the current URL of the given context via getTree(depth=0). */
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

    /** Navigate the given top-level context to url. */
    private void navigate(String contextId, String url) {
        try {
            browser.getWebDriver().browsingContext().navigate(url, contextId);
        } catch (Throwable ignore) { /* keep non-fatal */ }
    }

    /** Consider only root contexts; ignore iframes. */
    private boolean isTopLevel(String contextId) {
        try {
            WDBrowsingContextResult.GetTreeResult tree =
                    browser.getWebDriver().browsingContext()
                            .getTree(new WDBrowsingContext(contextId), 0L);
            if (tree.getContexts().isEmpty()) return true;
            return tree.getContexts().iterator().next().getParent() == null;
        } catch (Throwable t) {
            return true; // be permissive
        }
    }

    /** Choose a user for auto-login. Use same fallback strategy as elsewhere. */
    private UserRegistry.User pickUserForAutoLogin() {
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        return all.isEmpty() ? null : all.get(0);
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static boolean looksLikeLogin(String current, String configuredLogin) {
        if (current == null || current.isEmpty()) return false;
        if (configuredLogin != null && configuredLogin.length() > 0 && current.startsWith(configuredLogin)) return true;

        String c = current.toLowerCase(Locale.ROOT);
        return c.contains("/login") || c.contains("/signin") || c.contains("redirect=");
    }
}
