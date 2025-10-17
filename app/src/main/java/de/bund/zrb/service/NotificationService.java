package de.bund.zrb.service;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Zentraler Service für PrimeFaces-Growl-Notifications.
 * - Registriert sich direkt am BrowserImpl.onNotificationEvent(..)  ← WICHTIG
 * - Bietet await(..) mit Regex & Timeout.
 * - Konsumierte Meldungen werden für Popups unterdrückt (shouldPopup(..)).
 * - GlobalListener für UI (Swing-Popup o.ä.) laufen über diesen Service.
 */
public class NotificationService {

    // ===== Singleton pro Key (Page oder BrowsingContext/PageImpl) =====
    private static final Map<Object, NotificationService> INSTANCES = new ConcurrentHashMap<>();

    public static synchronized NotificationService getInstance(Object key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null! Use Page or Context.");
        return INSTANCES.computeIfAbsent(key, NotificationService::new);
    }
    public static synchronized void remove(Object key) { INSTANCES.remove(key); }

    // ===== Global Wiring (Browser ↔ Service) =====
    private static final Set<BrowserImpl> HOOKED_BROWSERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static final List<Consumer<GrowlNotification>> GLOBAL_LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Muss EINMAL pro BrowserImpl aufgerufen werden.
     * Leitet alle Events vom Browser in die passenden Service-Instanzen weiter.
     */
    public static synchronized void attachBrowser(BrowserImpl browser) {
        if (browser == null || HOOKED_BROWSERS.contains(browser)) return;
        HOOKED_BROWSERS.add(browser);

        browser.onNotificationEvent((WDScriptEvent.MessageWD msg) -> {
            GrowlNotification n = mapGrowlMessage(msg);
            if (n == null) return;

            // Key ermitteln: PageImpl zu Context-ID
            PageImpl page = browser.getPage(new WDBrowsingContext(n.contextId));
            Object key = (page != null) ? page : n.contextId; // Fallback: contextId

            NotificationService svc = NotificationService.getInstance(key);
            svc.accept(n);                      // interne Verarbeitung (await, Buffer, consume, listeners)
            // Danach globale Listener informieren (für UI etc.)
            for (Consumer<GrowlNotification> gl : GLOBAL_LISTENERS) {
                try { gl.accept(n); } catch (Throwable ignore) {}
            }
        });
    }

    /** Für dein Popup-Util o. ä.: hängt globale Anzeige an den Service. */
    public static void addGlobalListener(Consumer<GrowlNotification> l) {
        if (l != null) GLOBAL_LISTENERS.add(l);
    }
    public static void removeGlobalListener(Consumer<GrowlNotification> l) {
        GLOBAL_LISTENERS.remove(l);
    }

    // ===== Instanz-State =====
    private final Object key;
    private final List<GrowlNotification> buffer = new ArrayList<>();
    private final List<Consumer<List<GrowlNotification>>> listeners = new ArrayList<>();

    // Pending Waiters (await…)
    private static final class Pending {
        final Predicate<GrowlNotification> match;
        final CompletableFuture<GrowlNotification> future;
        final boolean consumeForPopup;
        Pending(Predicate<GrowlNotification> match, boolean consumeForPopup) {
            this.match = match;
            this.consumeForPopup = consumeForPopup;
            this.future = new CompletableFuture<>();
        }
    }
    private final List<Pending> waiters = new ArrayList<>();

    // „Verbrauchte“ Meldungen (gegen Doppel-Popup)
    // Key = signature(context|type|title|message), Value = timestamp
    private final ConcurrentHashMap<String, Long> consumed = new ConcurrentHashMap<>();
    private static final long CONSUME_TTL_MS = 60_000; // 1 min Hygiene

    private NotificationService(Object key) { this.key = key; }

    // ===== Öffentliche Listener API für Panels/Logs =====
    public synchronized void addListener(Consumer<List<GrowlNotification>> l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }
    public synchronized void removeListener(Consumer<List<GrowlNotification>> l) { listeners.remove(l); }
    public synchronized List<GrowlNotification> getAll() { return new ArrayList<>(buffer); }
    public synchronized void clear() { buffer.clear(); notifyListeners(); }
    private void notifyListeners() {
        List<GrowlNotification> snapshot = new ArrayList<>(buffer);
        for (Consumer<List<GrowlNotification>> l : listeners) {
            try { l.accept(snapshot); } catch (Throwable ignore) {}
        }
    }

    // ===== Interne Annahme eines fertigen GrowlNotification-Objekts =====
    private void accept(GrowlNotification n) {
        Pending matched = null;
        synchronized (this) {
            // 1) offene Waiter bedienen
            for (Pending p : waiters) {
                if (!p.future.isDone() && p.match.test(n)) {
                    matched = p;
                    break;
                }
            }
            if (matched != null) {
                matched.future.complete(n);
                if (matched.consumeForPopup) markConsumed(n);
                waiters.remove(matched);
            }

            // 2) immer in den Buffer (z. B. fürs Protokoll / UI)
            buffer.add(n);
            notifyListeners();
        }
        cleanupConsumed();
    }

    // ===== Blocking await mit Timeout =====
    /**
     * type: "INFO" | "WARN" | "ERROR" | "FATAL" | "ANY" | null
     * titleRegex / messageRegex: optional, null/leer = nicht filtern. Regex „find()“, nicht „matches()“.
     * consumeForPopup=true → Popup wird für diesen Treffer unterdrückt.
     */
    public GrowlNotification await(String type, String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        final Predicate<GrowlNotification> matcher = buildMatcher(type, titleRegex, messageRegex);
        return await(matcher, timeoutMs, true);
    }

    public GrowlNotification await(Predicate<GrowlNotification> matcher, long timeoutMs, boolean consumeForPopup)
            throws TimeoutException, InterruptedException, ExecutionException {
        Pending p = new Pending(matcher, consumeForPopup);

        // Schneller Treffer auf bereits gepufferte Meldungen
        synchronized (this) {
            for (GrowlNotification n : buffer) {
                if (matcher.test(n)) {
                    p.future.complete(n);
                    if (consumeForPopup) markConsumed(n);
                    break;
                }
            }
            if (!p.future.isDone()) {
                waiters.add(p);
            }
        }

        try {
            return p.future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            synchronized (this) { waiters.remove(p); }
            throw te;
        }
    }

    // ===== Popup-Kontrolle =====
    /** true = darf angezeigt werden; false = wurde von await(..) konsumiert. */
    public boolean shouldPopup(GrowlNotification n) {
        String sig = signature(n);
        Long ts = consumed.get(sig);
        if (ts == null) return true;
        if ((System.currentTimeMillis() - ts) > CONSUME_TTL_MS) {
            consumed.remove(sig);
            return true;
        }
        return false;
    }

    private void markConsumed(GrowlNotification n) {
        consumed.put(signature(n), System.currentTimeMillis());
    }
    private void cleanupConsumed() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : consumed.entrySet()) {
            if (now - e.getValue() > CONSUME_TTL_MS) consumed.remove(e.getKey());
        }
    }
    private static String signature(GrowlNotification n) {
        return (n.contextId + "|" + n.type + "|" + n.title + "|" + n.message);
    }

    // ===== Matcher & Mapping =====
    private static Predicate<GrowlNotification> buildMatcher(String type, String titleRegex, String messageRegex) {
        final String typeNorm =
                (type == null || type.trim().isEmpty() || "ANY".equalsIgnoreCase(type)) ? null : type.trim().toUpperCase();
        final PatternWrapper titleP = PatternWrapper.of(titleRegex);
        final PatternWrapper msgP   = PatternWrapper.of(messageRegex);

        return n -> {
            if (typeNorm != null && !typeNorm.equalsIgnoreCase(n.type)) return false;
            if (titleP != null && !titleP.matches(n.title)) return false;
            if (msgP != null && !msgP.matches(n.message)) return false;
            return true;
        };
    }

    // Mini-Wrapper, um Optional<Pattern> zu sparen
    private static final class PatternWrapper {
        private final java.util.regex.Pattern p;
        private PatternWrapper(java.util.regex.Pattern p) { this.p = p; }
        static PatternWrapper of(String regex) {
            if (regex == null || regex.trim().isEmpty()) return null;
            return new PatternWrapper(java.util.regex.Pattern.compile(regex));
        }
        boolean matches(String s) { return s != null && p.matcher(s).find(); }
    }

    // ----- Mapping: WDRemoteValue → GrowlNotification -----
    /** Optional nutzbar, falls du irgendwo noch das rohe WebSocket-Event bekommst. */
    public static GrowlNotification parse(WDScriptEvent.MessageWD msg) {
        return mapGrowlMessage(msg);
    }

    private static GrowlNotification mapGrowlMessage(WDScriptEvent.MessageWD msg) {
        WDRemoteValue payload = msg.getParams().getData();
        if (!(payload instanceof WDRemoteValue.ObjectRemoteValue)) return null;

        WDRemoteValue.ObjectRemoteValue root = (WDRemoteValue.ObjectRemoteValue) payload;
        String envelopeType = asString(getProp(root, "type"));
        if (!"growl-event".equals(envelopeType)) return null;

        WDRemoteValue dataVal = getProp(root, "data");
        if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return null;
        WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) dataVal;

        String type = asString(getProp(data, "type"));       // INFO|WARN|ERROR|FATAL
        String title = asString(getProp(data, "title"));
        String text  = asString(getProp(data, "message"));
        Long   ts    = asLong  (getProp(data, "timestamp"));

        if (type == null) type = "INFO";
        if (title == null) title = "";
        if (text == null)  text  = "";
        if (ts == null)    ts    = System.currentTimeMillis();

        String contextId = msg.getParams().getSource().getContext().value();
        return new GrowlNotification(contextId, type, title, text, ts);
    }

    private static WDRemoteValue getProp(WDRemoteValue.ObjectRemoteValue obj, String key) {
        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : obj.getValue().entrySet()) {
            if (e.getKey() instanceof WDPrimitiveProtocolValue.StringValue) {
                if (key.equals(((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue()))
                    return e.getValue();
            }
        }
        return null;
    }
    private static String asString(WDRemoteValue v) {
        if (v instanceof WDPrimitiveProtocolValue.StringValue) return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) return ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue) return String.valueOf(((WDPrimitiveProtocolValue.BooleanValue) v).getValue());
        return null;
    }
    private static Long asLong(WDRemoteValue v) {
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
            try { return Long.parseLong(((WDPrimitiveProtocolValue.NumberValue) v).getValue().split("\\.")[0]); }
            catch (Exception ignore) {}
        }
        return null;
    }
}
