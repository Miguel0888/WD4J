package de.bund.zrb.service;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDChannel;
import de.bund.zrb.type.script.WDChannelValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;
import de.bund.zrb.support.ScriptHelper;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Install the growl preload script, subscribe via onMessage, process notifications in two phases:
 *  A) matchers/await + programmatic handlers (may consume)
 *  B) unhandled sinks (e.g., Swing popup) if nothing consumed
 * Always record every event into history and notify snapshot listeners.
 */
public final class NotificationService {

    private static final String GROWL_CHANNEL = "notification-events-channel";
    private static final String GROWL_SCRIPT_PATH = "scripts/primeFacesGrowl.js";
    private static final int MAX_HISTORY = 1000;

    private static final Map<BrowserImpl, NotificationService> INSTANCES =
            new WeakHashMap<BrowserImpl, NotificationService>();
    private static final Set<BrowserImpl> PRELOAD_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    private final BrowserImpl browser;

    // Phase A: programmatic handlers that may consume events
    private final List<Predicate<GrowlNotification>> handlers = new CopyOnWriteArrayList<Predicate<GrowlNotification>>();
    // Phase B: unhandled sinks (only called when no one consumed)
    private final List<Consumer<GrowlNotification>> unhandledSinks = new CopyOnWriteArrayList<Consumer<GrowlNotification>>();
    // Snapshot listeners for tool UI
    private final List<Consumer<List<GrowlNotification>>> listeners = new CopyOnWriteArrayList<Consumer<List<GrowlNotification>>>();

    // History
    private final List<GrowlNotification> history = new ArrayList<GrowlNotification>();

    // Await waiters + consumed keys
    private final List<Waiter> waiters = new CopyOnWriteArrayList<Waiter>();
    private final Set<String> consumed = ConcurrentHashMap.newKeySet();

    private NotificationService(final BrowserImpl browser) {
        if (browser == null) throw new IllegalArgumentException("browser must not be null");
        this.browser = browser;

        ensurePreloadInstalled(browser);
        // Subscribe to the message bus
        this.browser.onMessage(new Consumer<WDScriptEvent.MessageWD>() {
            @Override
            public void accept(WDScriptEvent.MessageWD message) {
                try {
                    if (message == null || message.getParams() == null) return;
                    String channelValue = null;
                    try {
                        channelValue = message.getParams().getChannel() == null
                                ? null
                                : message.getParams().getChannel().value();
                    } catch (Throwable ignore) { }
                    if (!GROWL_CHANNEL.equals(channelValue)) return;

                    GrowlNotification dto = parseFromMessage(message);
                    if (dto == null) return;

                    onIncoming(dto);
                } catch (Throwable ignore) {
                    // Never break pipeline
                }
            }
        });
    }

    /** Install preload script once per browser (bind to growl channel). */
    private static synchronized void ensurePreloadInstalled(BrowserImpl browser) {
        if (browser == null) return;
        if (PRELOAD_INSTALLED.contains(browser)) return;
        try {
            String source = ScriptHelper.loadScript(GROWL_SCRIPT_PATH);
            browser.getScriptManager().addPreloadScript(
                    source,
                    java.util.Collections.singletonList(
                            new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(GROWL_CHANNEL)))
                    )
            );
            PRELOAD_INSTALLED.add(browser);
        } catch (Throwable t) {
            System.err.println("[NotificationService] Failed to install growl preload: " + t.getMessage());
        }
    }

    /** Get or create service instance bound to the BrowserImpl. */
    public static synchronized NotificationService getInstance(BrowserImpl browser) {
        NotificationService svc = INSTANCES.get(browser);
        if (svc == null) {
            svc = new NotificationService(browser);
            INSTANCES.put(browser, svc);
        }
        return svc;
    }

    /** Resolve by PageImpl for callers keyed by page. */
    public static NotificationService getInstance(PageImpl page) {
        if (page == null) throw new IllegalArgumentException("page must not be null");
        return getInstance(page.getBrowser());
    }

    // ---------- Public API: two-phase processing hooks ----------

    /** Register a programmatic handler. Return true to consume the event and stop phase B. */
    public void addHandler(Predicate<GrowlNotification> handler) {
        if (handler != null) handlers.add(handler);
    }
    public void removeHandler(Predicate<GrowlNotification> handler) { handlers.remove(handler); }

    /** Register an unhandled sink (called only if nothing consumed in phase A). */
    public void addSink(Consumer<GrowlNotification> sink) {
        if (sink != null) unhandledSinks.add(sink);
    }
    public void removeSink(Consumer<GrowlNotification> sink) { unhandledSinks.remove(sink); }

    /** Register a snapshot listener (tool window). Push current snapshot immediately. */
    public void addListener(Consumer<List<GrowlNotification>> listener) {
        if (listener == null) return;
        listeners.add(listener);
        listener.accept(getAll());
    }
    public void removeListener(Consumer<List<GrowlNotification>> listener) { listeners.remove(listener); }

    /** Return a defensive copy of the current history snapshot. */
    public List<GrowlNotification> getAll() {
        synchronized (this) { return new ArrayList<GrowlNotification>(history); }
    }

    /** Clear history and notify listeners. */
    public void clearHistory() {
        synchronized (this) { history.clear(); }
        notifySnapshotListeners();
    }

    /** Optional: tell whether popup may show (kept for compatibility). */
    public boolean shouldPopup(GrowlNotification n) {
        return !consumed.contains(keyOf(n));
    }

    /**
     * Wait for first matching notification and consume it to suppress phase B.
     * severity may be null, title/message are regex or null.
     */
    public GrowlNotification await(String severity,
                                   String titleRegex,
                                   String messageRegex,
                                   long timeoutMs)
            throws InterruptedException, TimeoutException, ExecutionException {

        final Waiter w = new Waiter(
                normalize(severity),
                compileOrNull(titleRegex),
                compileOrNull(messageRegex),
                true /* consume on match */);
        waiters.add(w);
        try {
            boolean ok = w.latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!ok) throw new TimeoutException("No growl matched within " + timeoutMs + " ms");
            return w.matched;
        } finally {
            waiters.remove(w);
        }
    }

    // ---------- Pipeline (two phases) ----------

    private void onIncoming(GrowlNotification n) {
        // Always record into history first
        synchronized (this) {
            history.add(n);
            if (history.size() > MAX_HISTORY) {
                int excess = history.size() - MAX_HISTORY;
                for (int i = 0; i < excess; i++) history.remove(0);
            }
        }

        boolean consumedByA = false;

        // Phase A1: await waiters
        for (Waiter w : waiters) {
            if (!w.done.get() && w.matches(n)) {
                w.matched = n;
                w.done.set(true);
                if (w.consume) {
                    consumed.add(keyOf(n)); // mark this exact event as consumed
                    consumedByA = true;
                }
                w.latch.countDown();
            }
        }

        // Phase A2: programmatic handlers
        if (!consumedByA) {
            for (Predicate<GrowlNotification> h : handlers) {
                try {
                    if (h != null && h.test(n)) {
                        consumed.add(keyOf(n));
                        consumedByA = true;
                        break; // stop at first consumer
                    }
                } catch (Throwable ignore) { }
            }
        }

        // Phase B: unhandled sinks only if nothing consumed
        if (!consumedByA) {
            for (Consumer<GrowlNotification> sink : unhandledSinks) {
                try { sink.accept(n); } catch (Throwable ignore) { }
            }
        }

        notifySnapshotListeners();
    }

    private void notifySnapshotListeners() {
        List<GrowlNotification> snap = getAll();
        for (Consumer<List<GrowlNotification>> l : listeners) {
            try { l.accept(snap); } catch (Throwable ignore) { }
        }
    }

    // ---------- Mapping (RecorderService-Stil) ----------

    public static GrowlNotification parseFromMessage(WDScriptEvent.MessageWD message) {
        if (message == null || message.getParams() == null) return null;

        WDRemoteValue dataVal = message.getParams().getData();
        if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return null;
        WDRemoteValue.ObjectRemoteValue env = (WDRemoteValue.ObjectRemoteValue) dataVal;

        String envelopeType = null;
        WDRemoteValue.ObjectRemoteValue dataObj = null;
        String contextId = null;

        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : env.getValue().entrySet()) {
            if (!(e.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;
            String key = ((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue();
            WDRemoteValue val = e.getValue();

            if ("type".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                envelopeType = ((WDPrimitiveProtocolValue.StringValue) val).getValue();
                continue;
            }
            if ("data".equals(key) && val instanceof WDRemoteValue.ObjectRemoteValue) {
                dataObj = (WDRemoteValue.ObjectRemoteValue) val;
                continue;
            }
            if ("contextId".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                contextId = ((WDPrimitiveProtocolValue.StringValue) val).getValue();
            }
        }

        if (!"growl-event".equals(envelopeType) || dataObj == null) return null;

        GrowlNotification dto = new GrowlNotification();
        String ts = null;

        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : dataObj.getValue().entrySet()) {
            if (!(e.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;
            String key = ((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue();
            WDRemoteValue val = e.getValue();

            if ("type".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                dto.type = normalize(((WDPrimitiveProtocolValue.StringValue) val).getValue());
                continue;
            }
            if ("title".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                dto.title = safe(((WDPrimitiveProtocolValue.StringValue) val).getValue());
                continue;
            }
            if ("message".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                dto.message = safe(((WDPrimitiveProtocolValue.StringValue) val).getValue());
                continue;
            }
            if ("timestamp".equals(key)) {
                if (val instanceof WDPrimitiveProtocolValue.StringValue) {
                    ts = ((WDPrimitiveProtocolValue.StringValue) val).getValue();
                } else if (val instanceof WDPrimitiveProtocolValue.NumberValue) {
                    ts = String.valueOf(((WDPrimitiveProtocolValue.NumberValue) val).getValue());
                }
                continue;
            }
            if ("contextId".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                contextId = ((WDPrimitiveProtocolValue.StringValue) val).getValue();
            }
        }

        dto.timestamp = parseLong(ts);
        if (dto.timestamp == 0L) {
            // Fallback for safety if script forgot timestamp
            dto.timestamp = System.currentTimeMillis();
        }
        dto.contextId = contextId;

        if ((dto.title == null || dto.title.length() == 0) &&
                (dto.message == null || dto.message.length() == 0)) {
            return null;
        }
        return dto;
    }

    // ---------- Helpers ----------

    private static String keyOf(GrowlNotification n) {
        // Include timestamp to distinguish equal text events in time
        StringBuilder b = new StringBuilder();
        b.append(n.contextId == null ? "" : n.contextId).append('|');
        b.append(n.type == null ? "" : n.type).append('|');
        b.append(n.title == null ? "" : n.title).append('|');
        b.append(n.message == null ? "" : n.message).append('|');
        b.append(n.timestamp);
        return b.toString();
    }

    private static String normalize(String s) { return s == null ? null : s.trim().toUpperCase(); }
    private static String safe(String s)      { return s == null ? "" : s; }

    private static long parseLong(String s) {
        if (s == null) return 0L;
        try { return Long.parseLong(s.trim()); } catch (Exception ignore) { return 0L; }
    }

    private static Pattern compileOrNull(String regex) throws ExecutionException {
        if (regex == null || regex.trim().isEmpty()) return null;
        try { return Pattern.compile(regex); }
        catch (Exception ex) { throw new ExecutionException("Invalid regex: " + regex, ex); }
    }

    // --- await waiter ---
    private static final class Waiter {
        final String severity;
        final Pattern titleRegex;
        final Pattern messageRegex;
        final boolean consume;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);
        volatile GrowlNotification matched;

        Waiter(String severity, Pattern titleRegex, Pattern messageRegex, boolean consume) {
            this.severity = severity;
            this.titleRegex = titleRegex;
            this.messageRegex = messageRegex;
            this.consume = consume;
        }

        boolean matches(GrowlNotification n) {
            if (severity != null) {
                String t = n.type == null ? "" : n.type.toUpperCase();
                if (!severity.equals(t)) return false;
            }
            if (titleRegex != null) {
                String t = n.title == null ? "" : n.title;
                if (!titleRegex.matcher(t).find()) return false;
            }
            if (messageRegex != null) {
                String m = n.message == null ? "" : n.message;
                if (!messageRegex.matcher(m).find()) return false;
            }
            return true;
        }
    }

    /** Checked timeout to avoid ambiguity with java.util.concurrent. */
    public static final class TimeoutException extends Exception {
        public TimeoutException(String msg) { super(msg); }
    }
}
