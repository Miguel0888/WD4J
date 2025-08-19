package de.bund.zrb.service;

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

    /** Aktualisiert die Aktivierungsmatrix und registriert/entfernt Event-Handler differenziert. */
    void update(Map<WDEventNames, Boolean> newEnabledFlags) {
        if (newEnabledFlags == null || newEnabledFlags.isEmpty()) return;

        // 1) Page-Teil updaten
        if (page != null) {
            applyDeltaForPage(newEnabledFlags);
            // WD-Extension-Page-Teil
            applyDeltaForExtensionPage(newEnabledFlags);
        }

        // 2) Context-Teil updaten (inkl. WD-Extension)
        if (ctx != null) {
            applyDeltaForContext(newEnabledFlags);
            applyDeltaForExtensionContext(newEnabledFlags);

            // 3) Auf Children durchreichen
            for (WDUiAppender child : childPageAppenders) {
                child.update(newEnabledFlags);
            }
        }

        // 4) enabled-Map übernehmen
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
        // Alle als true markierten Page-Events registrieren
        for (Map.Entry<WDEventNames, Boolean> e : enabled.entrySet()) {
            if (TRUE(e.getValue())) attachPageEvent(e.getKey());
        }
        // WD-Extension Page (z. B. FRAGMENT_NAVIGATED)
        attachExtensionPageIfEnabled(WDEventNames.FRAGMENT_NAVIGATED);
    }

    private void attachInitialContext() {
        // Alle als true markierten Context-Events registrieren
        for (Map.Entry<WDEventNames, Boolean> e : enabled.entrySet()) {
            if (TRUE(e.getValue())) attachContextEvent(e.getKey());
        }
        // WD-Extension Context
        attachExtensionContextIfEnabled(WDEventNames.FRAGMENT_NAVIGATED);

        // Existierende Seiten anhängen (nur WDPageExtension) + zukünftig kommende
        for (Page p : ctx.pages()) {
            if (p instanceof WDPageExtension) {
                childPageAppenders.add(attachToPage(p, sink, config, enabled));
            }
        }
        onPageHook = new Consumer<Page>() {
            @Override public void accept(Page p) {
                if (p instanceof WDPageExtension) {
                    childPageAppenders.add(attachToPage(p, sink, config, enabled));
                }
            }
        };
        ctx.onPage(onPageHook);
    }

    // ------------------ Delta-Anwendung ------------------

    private void applyDeltaForPage(Map<WDEventNames, Boolean> newEnabled) {
        for (Map.Entry<WDEventNames, BiFunction<Page, BiConsumer<String,Object>, Consumer<?>>> e
                : config.getAttachPage().entrySet()) {

            WDEventNames ev = e.getKey();
            boolean oldVal = TRUE(enabled.get(ev));
            boolean newVal = TRUE(newEnabled.get(ev));
            if (oldVal == newVal) continue;

            if (newVal) attachPageEvent(ev); else detachPageEvent(ev);
        }
    }

    private void applyDeltaForContext(Map<WDEventNames, Boolean> newEnabled) {
        for (Map.Entry<WDEventNames, BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>>> e
                : config.getAttachContext().entrySet()) {

            WDEventNames ev = e.getKey();
            boolean oldVal = TRUE(enabled.get(ev));
            boolean newVal = TRUE(newEnabled.get(ev));
            if (oldVal == newVal) continue;

            if (newVal) attachContextEvent(ev); else detachContextEvent(ev);
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
        if (pageHandlers.containsKey(ev)) return; // bereits registriert
        BiFunction<Page, BiConsumer<String,Object>, Consumer<?>> on = config.getAttachPage().get(ev);
        if (on == null) return;
        Consumer<?> handler = on.apply(page, sink);
        if (handler != null) pageHandlers.put(ev, handler);
    }

    private void detachPageEvent(WDEventNames ev) {
        if (page == null) return;
        Consumer<?> h = pageHandlers.remove(ev);
        if (h == null) return;
        BiConsumer<Page, Consumer<?>> off = config.getDetachPage().get(ev);
        if (off != null) {
            try { off.accept(page, h); } catch (Throwable ignore) {}
        }
    }

    // ------------------ Attach/Detach: Context ------------------

    private void attachContextEvent(WDEventNames ev) {
        if (ctx == null) return;
        if (contextHandlers.containsKey(ev)) return;
        BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>> on = config.getAttachContext().get(ev);
        if (on == null) return;
        Consumer<?> handler = on.apply(ctx, sink);
        if (handler != null) contextHandlers.put(ev, handler);
    }

    private void detachContextEvent(WDEventNames ev) {
        if (ctx == null) return;
        Consumer<?> h = contextHandlers.remove(ev);
        if (h == null) return;
        BiConsumer<BrowserContext, Consumer<?>> off = config.getDetachContext().get(ev);
        if (off != null) {
            try { off.accept(ctx, h); } catch (Throwable ignore) {}
        }
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
