package de.bund.zrb.ui.debug;

import com.microsoft.playwright.*;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.ext.WDContextExtension;
import de.bund.zrb.ext.WDPageExtension;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Schlanker Appender:
 * - nutz WDEventWiringConfig (Attach/Detach-Lambdas) für Page- & Context-Events
 * - hat ein gemeinsames Enabled-Flag-Set (Map<WDEventNames, Boolean>) für Page/Context
 * - kann per update(newEnabled) differenziert nachregistrieren/abregistrieren
 * - verdrahtet optional WD-Extension-Events (z. B. FRAGMENT_NAVIGATED) über dasselbe Flag
 */
final class WDUiAppender {
    private final Page page;                         // Page-Mode oder null
    private final BrowserContext ctx;                // Context-Mode oder null
    private final BiConsumer<String, Object> sink;   // <BiDi-Eventname, Event>
    private final WDEventWiringConfig config;

    // Aktivierte Events (gemeinsam für Page + Context)
    private final EnumMap<WDEventNames, Boolean> enabled;

    // RAW event handlers for page and context. These maps store the external handler
    // associated with each event so that we can deregister them later. We use
    // Consumers of Object to handle raw WebDriver BiDi payloads.
    private final EnumMap<WDEventNames, java.util.function.Consumer<Object>> rawPageHandlers = new EnumMap<WDEventNames, java.util.function.Consumer<Object>>(WDEventNames.class);
    private final EnumMap<WDEventNames, java.util.function.Consumer<Object>> rawContextHandlers = new EnumMap<WDEventNames, java.util.function.Consumer<Object>>(WDEventNames.class);

    // Registrierte Handler (zum gezielten Abhängen)
    private final EnumMap<WDEventNames, Consumer<?>> pageHandlers = new EnumMap<WDEventNames, Consumer<?>>(WDEventNames.class);
    private final EnumMap<WDEventNames, Consumer<?>> contextHandlers = new EnumMap<WDEventNames, Consumer<?>>(WDEventNames.class);

    // WD-Extension-Detacher (pro Event)
    private final EnumMap<WDEventNames, Runnable> extPageDetachers = new EnumMap<WDEventNames, Runnable>(WDEventNames.class);
    private final EnumMap<WDEventNames, Runnable> extContextDetachers = new EnumMap<WDEventNames, Runnable>(WDEventNames.class);

    // Context-Mode: Child-Page-Appender + onPage-Hook
    private final List<WDUiAppender> childPageAppenders = new ArrayList<WDUiAppender>();
    private Consumer<Page> onPageHook;

    // ------------------ Konstruktoren (privat) ------------------

    private WDUiAppender(Page page,
                         BiConsumer<String, Object> sink,
                         WDEventWiringConfig config,
                         Map<WDEventNames, Boolean> enabledFlags) {
        this.page = page;
        this.ctx = null;
        this.sink = sink != null ? sink : new BiConsumer<String, Object>() { @Override public void accept(String n, Object o) {} };
        this.config = config != null ? config : WDEventWiringConfig.defaults();
        this.enabled = copyEnabled(enabledFlags);
        // initial verdrahten (nur Page-Teil + evtl. Extension)
        attachInitialPage();
    }

    private WDUiAppender(BrowserContext ctx,
                         BiConsumer<String, Object> sink,
                         WDEventWiringConfig config,
                         Map<WDEventNames, Boolean> enabledFlags) {
        this.page = null;
        this.ctx = ctx;
        this.sink = sink != null ? sink : new BiConsumer<String, Object>() { @Override public void accept(String n, Object o) {} };
        this.config = config != null ? config : WDEventWiringConfig.defaults();
        this.enabled = copyEnabled(enabledFlags);
        // initial verdrahten (nur Context-Teil + evtl. Extension) und Child-Pages
        attachInitialContext();
    }

    private static EnumMap<WDEventNames, Boolean> copyEnabled(Map<WDEventNames, Boolean> src) {
        EnumMap<WDEventNames, Boolean> map = new EnumMap<WDEventNames, Boolean>(WDEventNames.class);
        if (src != null) map.putAll(src);
        return map;
    }

    // ------------------ Fabriken ------------------

    static WDUiAppender attachToPage(Page page,
                                     BiConsumer<String, Object> sink,
                                     WDEventWiringConfig config,
                                     Map<WDEventNames, Boolean> enabledFlags) {
        return new WDUiAppender(page, sink, config, enabledFlags);
    }

    static WDUiAppender attachToContext(BrowserContext ctx,
                                        BiConsumer<String, Object> sink,
                                        WDEventWiringConfig config,
                                        Map<WDEventNames, Boolean> enabledFlags) {
        return new WDUiAppender(ctx, sink, config, enabledFlags);
    }

    // ------------------ Public API ------------------

    /**
     * Updates the internal enabled-flag map for UI filtering. Raw event handlers are
     * always registered and therefore are <em>not</em> attached or detached based on the
     * new flags. Only extension-based handlers may be updated via delta methods if
     * needed. Child appenders receive the same flag updates.
     *
     * @param newEnabledFlags the updated flags map; ignored if null or empty
     */
    void update(Map<WDEventNames, Boolean> newEnabledFlags) {
        if (newEnabledFlags == null || newEnabledFlags.isEmpty()) return;
        // For raw events we no longer attach/detach on flag change. However, we still allow
        // extension event handlers (e.g. fragment navigated) to be updated if needed.
        if (page != null) {
            // Only update extension events (fragment navigated) based on flags
            // Other Playwright events are ignored to avoid double wiring
            applyDeltaForExtensionPage(newEnabledFlags);
        }
        if (ctx != null) {
            // Update extension events on the context
            applyDeltaForExtensionContext(newEnabledFlags);
            for (WDUiAppender child : childPageAppenders) {
                child.update(newEnabledFlags);
            }
        }
        enabled.clear();
        enabled.putAll(newEnabledFlags);
    }

    /** Hängt alles ab: eigene Handler, Extension-Handler und Kinder. */
    void detachAll() {
        // Page: alle bekannten Events abhängen
        if (page != null) {
            for (Map.Entry<WDEventNames, Consumer<?>> e : pageHandlers.entrySet()) {
                WDEventNames ev = e.getKey();
                Consumer<?> h = e.getValue();
                BiConsumer<Page, Consumer<?>> off = config.getDetachPage().get(ev);
                if (off != null) {
                    try { off.accept(page, h); } catch (Throwable ignore) {}
                }
            }
            pageHandlers.clear();

            // RAW Page: remove all registered raw handlers
            if (page instanceof WDPageExtension) {
                for (Map.Entry<WDEventNames, java.util.function.Consumer<Object>> e : rawPageHandlers.entrySet()) {
                    try { ((WDPageExtension) page).offRaw(e.getKey(), e.getValue()); } catch (Throwable ignore) {}
                }
            }
            rawPageHandlers.clear();

            // Extension-Page-Detacher
            for (Runnable r : extPageDetachers.values()) {
                try { r.run(); } catch (Throwable ignore) {}
            }
            extPageDetachers.clear();
        }

        // Context: alle bekannten Events abhängen
        if (ctx != null) {
            for (Map.Entry<WDEventNames, Consumer<?>> e : contextHandlers.entrySet()) {
                WDEventNames ev = e.getKey();
                Consumer<?> h = e.getValue();
                BiConsumer<BrowserContext, Consumer<?>> off = config.getDetachContext().get(ev);
                if (off != null) {
                    try { off.accept(ctx, h); } catch (Throwable ignore) {}
                }
            }
            contextHandlers.clear();

            // RAW Context: remove all registered raw handlers
            if (ctx instanceof WDContextExtension) {
                for (Map.Entry<WDEventNames, java.util.function.Consumer<Object>> e : rawContextHandlers.entrySet()) {
                    try { ((WDContextExtension) ctx).offRaw(e.getKey(), e.getValue()); } catch (Throwable ignore) {}
                }
            }
            rawContextHandlers.clear();

            // Extension-Context-Detacher
            for (Runnable r : extContextDetachers.values()) {
                try { r.run(); } catch (Throwable ignore) {}
            }
            extContextDetachers.clear();

            // Child-Appendere abhängen
            for (WDUiAppender c : childPageAppenders) {
                try { c.detachAll(); } catch (Throwable ignore) {}
            }
            childPageAppenders.clear();

            // onPage-Hook abhängen
            if (onPageHook != null) {
                try { ctx.offPage(onPageHook); } catch (Throwable ignore) {}
                onPageHook = null;
            }
        }
    }

    // ------------------ Initialattachs ------------------

    private void attachInitialPage() {
        // Always attach raw event handlers for all possible events. This ensures that
        // recording of events does not depend on UI filter flags. If the page
        // implements the WDPageExtension, subscribe to each event via onRaw().
        if (page instanceof WDPageExtension) {
            for (WDEventNames ev : WDEventNames.values()) {
                // Register raw only once per event
                if (!rawPageHandlers.containsKey(ev)) {
                    attachRawPageEvent(ev);
                }
            }
        }
        // For events that are enabled in the current flag set, also attach
        // Playwright-level handlers via the wiring config. This acts as a
        // fallback for events that are not supported by the raw interface.
        for (Map.Entry<WDEventNames, Boolean> e : enabled.entrySet()) {
            if (!TRUE(e.getValue())) continue;
            WDEventNames ev = e.getKey();
            // Only attach Playwright wiring if raw was not attached or the
            // wiring config provides additional event types. The attachPageEvent
            // method checks internally if a handler already exists.
            attachPageEvent(ev);
        }
        // Also attach extension events (e.g. FRAGMENT_NAVIGATED) for enabled
        // flags. These may provide typed events beyond raw events.
        applyDeltaForExtensionPage(enabled);
    }

    private void attachInitialContext() {
        // Always attach raw handlers for all event names on the context. This ensures
        // complete recording regardless of UI filter settings. If the context
        // implements WDContextExtension, subscribe to every event via onRaw().
        if (ctx instanceof WDContextExtension) {
            for (WDEventNames ev : WDEventNames.values()) {
                if (!rawContextHandlers.containsKey(ev)) {
                    attachRawContextEvent(ev);
                }
            }
        }
        // For events enabled in the current flag set, attach Playwright wiring as a
        // fallback. This supports events that may not be covered by the raw API.
        for (Map.Entry<WDEventNames, Boolean> e : enabled.entrySet()) {
            if (!TRUE(e.getValue())) continue;
            WDEventNames ev = e.getKey();
            attachContextEvent(ev);
        }
        // Attach extension events (fragment navigated) for enabled flags.
        applyDeltaForExtensionContext(enabled);
        // Attach existing pages to inherit context-level subscriptions for those pages.
        // New pages added to the context will be automatically attached via onPageHook.
        for (Page p : ctx.pages()) {
            if (p instanceof WDPageExtension || config.getAttachPage().containsKey(WDEventNames.CONTEXT_CREATED)) {
                childPageAppenders.add(attachToPage(p, sink, config, enabled));
            }
        }
        onPageHook = new Consumer<Page>() {
            @Override public void accept(Page p) {
                // Attach to new pages that support raw events or have a Playwright wiring.
                if (p instanceof WDPageExtension || config.getAttachPage().containsKey(WDEventNames.CONTEXT_CREATED)) {
                    childPageAppenders.add(attachToPage(p, sink, config, enabled));
                }
            }
        };
        ctx.onPage(onPageHook);
    }

    // ------------------ Delta-Anwendung ------------------

    private void applyDeltaForPage(Map<WDEventNames, Boolean> newEnabled) {
        // For each event compare old and new enabled flags and attach/detach handlers. Prefer
        // raw handlers when available; fall back to Playwright wiring otherwise. We only
        // register one handler per event: raw OR Playwright, not both.
        for (WDEventNames ev : WDEventNames.values()) {
            boolean oldVal = TRUE(enabled.get(ev));
            boolean newVal = TRUE(newEnabled.get(ev));
            if (oldVal == newVal) continue;
            if (newVal) {
                boolean rawAttached = false;
                if (page instanceof WDPageExtension) {
                    if (!rawPageHandlers.containsKey(ev)) {
                        attachRawPageEvent(ev);
                        rawAttached = rawPageHandlers.containsKey(ev);
                    }
                }
                if (!rawAttached) {
                    attachPageEvent(ev);
                }
            } else {
                // Detach whichever handler was previously attached
                // Prefer raw detach if present
                boolean detached = false;
                if (rawPageHandlers.containsKey(ev)) {
                    detachRawPageEvent(ev);
                    detached = true;
                }
                // Only detach the Playwright handler if no raw was detached or if a
                // Playwright handler is also present. This avoids detaching unrelated
                // handlers when both types existed.
                if (!detached || pageHandlers.containsKey(ev)) {
                    detachPageEvent(ev);
                }
            }
        }
    }

    private void applyDeltaForContext(Map<WDEventNames, Boolean> newEnabled) {
        // For each event compare old and new enabled flags and attach/detach handlers. Prefer
        // raw handlers when available; fall back to Playwright wiring otherwise.
        for (WDEventNames ev : WDEventNames.values()) {
            boolean oldVal = TRUE(enabled.get(ev));
            boolean newVal = TRUE(newEnabled.get(ev));
            if (oldVal == newVal) continue;
            if (newVal) {
                boolean rawAttached = false;
                if (ctx instanceof WDContextExtension) {
                    if (!rawContextHandlers.containsKey(ev)) {
                        attachRawContextEvent(ev);
                        rawAttached = rawContextHandlers.containsKey(ev);
                    }
                }
                if (!rawAttached) {
                    attachContextEvent(ev);
                }
            } else {
                boolean detached = false;
                if (rawContextHandlers.containsKey(ev)) {
                    detachRawContextEvent(ev);
                    detached = true;
                }
                if (!detached || contextHandlers.containsKey(ev)) {
                    detachContextEvent(ev);
                }
            }
        }
    }

    private void applyDeltaForExtensionPage(Map<WDEventNames, Boolean> newEnabled) {
        // aktuell nur FRAGMENT_NAVIGATED als Beispiel
        WDEventNames ev = WDEventNames.FRAGMENT_NAVIGATED;
        boolean oldVal = TRUE(enabled.get(ev));
        boolean newVal = TRUE(newEnabled.get(ev));
        if (oldVal == newVal) return;

        if (newVal) attachExtensionPageIfEnabled(ev); else detachExtensionPage(ev);
    }

    private void applyDeltaForExtensionContext(Map<WDEventNames, Boolean> newEnabled) {
        WDEventNames ev = WDEventNames.FRAGMENT_NAVIGATED;
        boolean oldVal = TRUE(enabled.get(ev));
        boolean newVal = TRUE(newEnabled.get(ev));
        if (oldVal == newVal) return;

        if (newVal) attachExtensionContextIfEnabled(ev); else detachExtensionContext(ev);
    }

    // ------------------ Attach/Detach: Page ------------------

    private void attachPageEvent(WDEventNames ev) {
        if (page == null) return;
        if (pageHandlers.containsKey(ev)) return; // already registered
        final BiFunction<Page, BiConsumer<String,Object>, Consumer<?>> on = config.getAttachPage().get(ev);
        if (on == null) return;

        try {
            // Subscribe synchronously; if the browser does not support it, this will throw here.
            final Consumer<?> handler = on.apply(page, sink);
            if (handler != null) {
                pageHandlers.put(ev, handler);
            }
        } catch (Throwable t) {
            // Do not let one event abort others; just log and continue.
            System.err.println("[WDUiAppender] PAGE subscribe failed for " + ev.getName() + " : " + t);
        }
    }

    private void detachPageEvent(WDEventNames ev) {
        if (page == null) return;
        final Consumer<?> h = pageHandlers.remove(ev);
        if (h == null) return;
        final BiConsumer<Page, Consumer<?>> off = config.getDetachPage().get(ev);
        if (off != null) {
            try { off.accept(page, h); } catch (Throwable ignore) {}
        }
    }

    // ------------------ Attach/Detach: Context ------------------

    private void attachContextEvent(WDEventNames ev) {
        if (ctx == null) return;
        if (contextHandlers.containsKey(ev)) return; // already registered
        final BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>> on = config.getAttachContext().get(ev);
        if (on == null) return;

        try {
            // Subscribe synchronously; if the browser does not support it, this will throw here.
            final Consumer<?> handler = on.apply(ctx, sink);
            if (handler != null) {
                contextHandlers.put(ev, handler);
            }
        } catch (Throwable t) {
            // Keep going; do not abort wiring for other events.
            System.err.println("[WDUiAppender] CTX  subscribe failed for " + ev.getName() + " : " + t);
        }
    }

    private void detachContextEvent(WDEventNames ev) {
        if (ctx == null) return;
        final Consumer<?> h = contextHandlers.remove(ev);
        if (h == null) return;
        final BiConsumer<BrowserContext, Consumer<?>> off = config.getDetachContext().get(ev);
        if (off != null) {
            try { off.accept(ctx, h); } catch (Throwable ignore) {}
        }
    }

    // ------------------ RAW Attach/Detach: Page ------------------

    /**
     * Attaches a raw event handler for the given event on the page if possible. Only
     * pages implementing {@link WDPageExtension} support raw event subscription. If
     * the event is already attached it will not be added again.
     *
     * @param ev the event name
     */
    // ------------------ RAW Attach/Detach: Page ------------------

    private void attachRawPageEvent(final WDEventNames ev) {
        if (page == null) return;
        if (!(page instanceof WDPageExtension)) return;
        if (rawPageHandlers.containsKey(ev)) return;

        final WDPageExtension ext = (WDPageExtension) page;
        final java.util.function.Consumer<Object> h = new java.util.function.Consumer<Object>() {
            @Override public void accept(Object obj) { sink.accept(ev.getName(), obj); }
        };

        try {
            // Subscribe synchronously; throw here if unsupported -> catch below.
            ext.onRaw(ev, h);
            rawPageHandlers.put(ev, h);
        } catch (Throwable t) {
            // Log and keep wiring other events.
            System.err.println("[WDUiAppender] PAGE raw subscribe failed for " + ev.getName() + " : " + t);
        }
    }

    /**
     * Detaches the raw event handler for the given event on the page if one
     * exists. If the handler was not previously attached this method does
     * nothing.
     *
     * @param ev the event name
     */
    private void detachRawPageEvent(WDEventNames ev) {
        if (page == null) return;
        if (!(page instanceof WDPageExtension)) return;
        java.util.function.Consumer<Object> h = rawPageHandlers.remove(ev);
        if (h == null) return;
        ((WDPageExtension) page).offRaw(ev, h);
    }

    // ------------------ RAW Attach/Detach: Context ------------------

    /**
     * Attaches a raw event handler for the given event on the context if possible. Only
     * contexts implementing {@link WDContextExtension} support raw event subscription. If
     * the event is already attached it will not be added again.
     *
     * @param ev the event name
     */
    private void attachRawContextEvent(final WDEventNames ev) {
        if (ctx == null) return;
        if (!(ctx instanceof WDContextExtension)) return;
        if (rawContextHandlers.containsKey(ev)) return;

        final WDContextExtension ext = (WDContextExtension) ctx;
        final java.util.function.Consumer<Object> h = new java.util.function.Consumer<Object>() {
            @Override public void accept(Object obj) { sink.accept(ev.getName(), obj); }
        };

        try {
            // Subscribe synchronously; throw here if unsupported -> catch below.
            ext.onRaw(ev, h);
            rawContextHandlers.put(ev, h);
        } catch (Throwable t) {
            // Log and continue; never block subsequent registrations.
            System.err.println("[WDUiAppender] CTX  raw subscribe failed for " + ev.getName() + " : " + t);
        }
    }

    /**
     * Detaches the raw event handler for the given event on the context if one
     * exists. If the handler was not previously attached this method does
     * nothing.
     *
     * @param ev the event name
     */
    private void detachRawContextEvent(WDEventNames ev) {
        if (ctx == null) return;
        if (!(ctx instanceof WDContextExtension)) return;
        java.util.function.Consumer<Object> h = rawContextHandlers.remove(ev);
        if (h == null) return;
        ((WDContextExtension) ctx).offRaw(ev, h);
    }

    // ------------------ WD-Extension: Page ------------------

    private void attachExtensionPageIfEnabled(WDEventNames ev) {
        if (page == null) return;
        if (!(page instanceof WDPageExtension)) return;
        if (!TRUE(enabled.get(ev))) return;
        if (extPageDetachers.containsKey(ev)) return;

        final WDPageExtension ext = (WDPageExtension) page;

        if (ev == WDEventNames.FRAGMENT_NAVIGATED) {
            final Consumer<WDBrowsingContextEvent.FragmentNavigated> h =
                    new Consumer<WDBrowsingContextEvent.FragmentNavigated>() {
                        @Override public void accept(WDBrowsingContextEvent.FragmentNavigated e) {
                            sink.accept(WDEventNames.FRAGMENT_NAVIGATED.getName(), e);
                        }
                    };
            ext.onFragmentNavigated(h);
            extPageDetachers.put(ev, new Runnable() {
                @Override public void run() { ext.offFragmentNavigated(h); }
            });
        }
    }

    private void detachExtensionPage(WDEventNames ev) {
        Runnable r = extPageDetachers.remove(ev);
        if (r != null) {
            try { r.run(); } catch (Throwable ignore) {}
        }
    }

    // ------------------ WD-Extension: Context ------------------

    private void attachExtensionContextIfEnabled(WDEventNames ev) {
        if (ctx == null) return;
        if (!(ctx instanceof WDContextExtension)) return;
        if (!TRUE(enabled.get(ev))) return;
        if (extContextDetachers.containsKey(ev)) return;

        final WDContextExtension ext = (WDContextExtension) ctx;

        if (ev == WDEventNames.FRAGMENT_NAVIGATED) {
            final Consumer<WDBrowsingContextEvent.FragmentNavigated> h =
                    new Consumer<WDBrowsingContextEvent.FragmentNavigated>() {
                        @Override public void accept(WDBrowsingContextEvent.FragmentNavigated e) {
                            sink.accept(WDEventNames.FRAGMENT_NAVIGATED.getName(), e);
                        }
                    };
            ext.onFragmentNavigated(h);
            extContextDetachers.put(ev, new Runnable() {
                @Override public void run() { ext.offFragmentNavigated(h); }
            });
        }
    }

    private void detachExtensionContext(WDEventNames ev) {
        Runnable r = extContextDetachers.remove(ev);
        if (r != null) {
            try { r.run(); } catch (Throwable ignore) {}
        }
    }

    // ------------------ Helpers ------------------

    private static boolean TRUE(Boolean b) { return b != null && b.booleanValue(); }
}
