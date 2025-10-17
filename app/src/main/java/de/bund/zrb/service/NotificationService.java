package de.bund.zrb.service;

import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class NotificationService {

    // ===== Singleton pro Key (Page oder BrowsingContext) =====
    private static final Map<Object, NotificationService> INSTANCES = new ConcurrentHashMap<>();
    public static synchronized NotificationService getInstance(Object key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null! Use Page or Context.");
        return INSTANCES.computeIfAbsent(key, NotificationService::new);
    }
    public static synchronized void remove(Object key) { INSTANCES.remove(key); }

    // ===== Globale Listener (z. B. Swing-Popup-Util) – werden NUR über den Service bedient =====
    private static final CopyOnWriteArrayList<Consumer<GrowlNotification>> GLOBAL_LISTENERS = new CopyOnWriteArrayList<>();
    public static void addGlobalListener(Consumer<GrowlNotification> l) {
        if (l != null) GLOBAL_LISTENERS.addIfAbsent(l);
    }
    public static void removeGlobalListener(Consumer<GrowlNotification> l) {
        GLOBAL_LISTENERS.remove(l);
    }

    // ===== State =====
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

    // ===== Listener API =====
    public synchronized void addListener(Consumer<List<GrowlNotification>> l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }
    public synchronized void removeListener(Consumer<List<GrowlNotification>> l) { listeners.remove(l); }
    public synchronized List<GrowlNotification> getAll() { return new ArrayList<>(buffer); }
    public synchronized void clear() { buffer.clear(); notifyListeners(); }
    private void notifyListeners() {
        List<GrowlNotification> snapshot = new ArrayList<>(buffer);
        for (Consumer<List<GrowlNotification>> l : listeners) l.accept(snapshot);
    }

    // ===== Entry point aus BrowserImpl.onNotificationEvent(...) =====
    public void onNotificationMessage(WDScriptEvent.MessageWD msg) {
        GrowlNotification n = mapGrowlMessage(msg);
        if (n == null) return;

        // 1) evtl. auf einen Waiter matchen und erfüllen
        Pending matched = null;
        synchronized (this) {
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

        // 3) Globale Listener NACH dem evtl. Consume informieren (Popup-Util etc.)
        for (Consumer<GrowlNotification> l : GLOBAL_LISTENERS) {
            try { l.accept(n); } catch (Throwable ignore) {}
        }

        cleanupConsumed();
    }

    // ===== Blocking await mit Timeout =====
    public GrowlNotification await(String type, String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        final Predicate<GrowlNotification> matcher = buildMatcher(type, titleRegex, messageRegex);
        return await(matcher, timeoutMs, true); // standard: verbraucht → kein Popup
    }

    public GrowlNotification await(Predicate<GrowlNotification> matcher, long timeoutMs, boolean consumeForPopup)
            throws TimeoutException, InterruptedException, ExecutionException {
        Pending p = new Pending(matcher, consumeForPopup);

        // Schneller Treffer auf bereits gepufferte Meldungen (z. B. wenn sie „knapp vorher“ kam)
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
    /** Wird vom Popup-Util verwendet: true = darf angezeigt werden; false = wurde für await „verbraucht“. */
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
        // "ANY" (oder leer) bedeutet: kein Type-Filter
        String t = (type == null) ? null : type.trim();
        final String typeNorm =
                (t == null || t.isEmpty() || "ANY".equalsIgnoreCase(t)) ? null : t.toUpperCase();

        final PatternWrapper titleP = PatternWrapper.of(titleRegex);
        final PatternWrapper msgP = PatternWrapper.of(messageRegex);

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
