package de.bund.zrb.service;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDChannel;
import de.bund.zrb.type.script.WDChannelValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

// Nur für Preload-Registrierung (Channel-Bindung)
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
import java.util.regex.Pattern;

/**
 * Install the growl preload script in the constructor, subscribe via onMessage,
 * filter by channel string only, map DTO like RecorderService, keep history,
 * notify listeners and sinks, and support await(..) with consumption.
 */
public final class NotificationService {

    private static final String GROWL_CHANNEL = "notification-events-channel";
    private static final String GROWL_SCRIPT_PATH = "scripts/primeFacesGrowl.js";

    // One instance per BrowserImpl
    private static final Map<BrowserImpl, NotificationService> INSTANCES =
            new WeakHashMap<BrowserImpl, NotificationService>();

    // Track per-browser preload installation to avoid duplicates
    private static final Set<BrowserImpl> PRELOAD_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    private final BrowserImpl browser;

    // Single-item sinks (UI/logs)
    private final List<Consumer<GrowlNotification>> sinks = new CopyOnWriteArrayList<Consumer<GrowlNotification>>();
    // Snapshot listeners (e.g., NotificationTestDialog)
    private final List<Consumer<List<GrowlNotification>>> listeners = new CopyOnWriteArrayList<Consumer<List<GrowlNotification>>>();

    // History of notifications
    private final List<GrowlNotification> history = new ArrayList<GrowlNotification>();
    private static final int MAX_HISTORY = 1000;

    // Consumed keys to suppress popups after await(..)
    private final Set<String> consumed = ConcurrentHashMap.newKeySet();
    // Await waiters
    private final List<Waiter> waiters = new CopyOnWriteArrayList<Waiter>();

    private NotificationService(final BrowserImpl browser) {
        if (browser == null) throw new IllegalArgumentException("browser must not be null");
        this.browser = browser;

        // 1) Ensure preload is installed (idempotent; per browser)
        ensurePreloadInstalled(browser);

        // 2) Subscribe to message bus; filter purely by channel string
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
                    } catch (Throwable ignore) {
                        // leave null
                    }
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
            // Load script source
            String source = ScriptHelper.loadScript(GROWL_SCRIPT_PATH);

            // Register preload against the growl channel
            // Note: We must provide a WDChannelValue so the runtime routes messages to that channel.
            browser.getScriptManager().addPreloadScript(
                    source,
                    java.util.Collections.singletonList(
                            new WDChannelValue(
                                    new WDChannelValue.ChannelProperties(new WDChannel(GROWL_CHANNEL))
                            )
                    )
            );

            PRELOAD_INSTALLED.add(browser);
        } catch (Throwable t) {
            // Fail safe but be loud in logs to catch misconfiguration
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

    // ---------- Public API ----------

    /** Register a sink receiving every single notification (e.g., popup util). */
    public void addSink(Consumer<GrowlNotification> sink) {
        if (sink != null) sinks.add(sink);
    }

    public void removeSink(Consumer<GrowlNotification> sink) {
        sinks.remove(sink);
    }

    /** Register a snapshot listener (NotificationTestDialog). Immediately push current snapshot. */
    public void addListener(Consumer<List<GrowlNotification>> listener) {
        if (listener == null) return;
        listeners.add(listener);
        // Push current snapshot on register
        listener.accept(getAll());
    }

    public void removeListener(Consumer<List<GrowlNotification>> listener) {
        listeners.remove(listener);
    }

    /** Return a defensive copy of the current history snapshot. */
    public List<GrowlNotification> getAll() {
        synchronized (this) {
            return new ArrayList<GrowlNotification>(history);
        }
    }

    /** Tell whether the notification is not consumed (so popup may show). */
    public boolean shouldPopup(GrowlNotification n) {
        return !consumed.contains(keyOf(n));
    }

    /**
     * Wait for first matching notification and consume it to suppress UI.
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

    // ---------- Pipeline ----------

    private void onIncoming(GrowlNotification n) {
        // 0) Update history first (so snapshot includes this event)
        synchronized (this) {
            history.add(n);
            // Trim if beyond cap
            if (history.size() > MAX_HISTORY) {
                int excess = history.size() - MAX_HISTORY;
                for (int i = 0; i < excess; i++) {
                    history.remove(0);
                }
            }
        }

        // 1) Wake waiters and consume on match
        for (Waiter w : waiters) {
            if (!w.done.get() && w.matches(n)) {
                w.matched = n;
                w.done.set(true);
                if (w.consume) consumed.add(keyOf(n));
                w.latch.countDown();
            }
        }

        // 2) Fan-out to single-item sinks
        for (Consumer<GrowlNotification> sink : sinks) {
            try { sink.accept(n); } catch (Throwable ignore) { }
        }

        // 3) Notify snapshot listeners with a fresh copy
        List<GrowlNotification> snap = getAll();
        for (Consumer<List<GrowlNotification>> l : listeners) {
            try { l.accept(snap); } catch (Throwable ignore) { }
        }
    }

    // ---------- Mapping (RecorderService-Stil) ----------

    /**
     * Expect params.data:
     * {
     *   "type": "growl-event",
     *   "data": { "type": "INFO|WARN|ERROR|FATAL", "title": "...", "message": "...", "timestamp": <string|number> },
     *   "contextId": "..."? // optional
     * }
     */
    public static GrowlNotification parseFromMessage(WDScriptEvent.MessageWD message) {
        if (message == null || message.getParams() == null) return null;

        WDRemoteValue dataVal = message.getParams().getData();
        if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return null;
        WDRemoteValue.ObjectRemoteValue env = (WDRemoteValue.ObjectRemoteValue) dataVal;

        String envelopeType = null;
        WDRemoteValue.ObjectRemoteValue dataObj = null;
        String contextId = null;

        // Iterate envelope like RecorderService
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

        // Iterate nested data object
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
        dto.contextId = contextId;

        // At least one of title/message required
        if ((dto.title == null || dto.title.length() == 0) &&
                (dto.message == null || dto.message.length() == 0)) {
            return null;
        }
        return dto;
    }

    // ---------- Helpers ----------

    private static String keyOf(GrowlNotification n) {
        StringBuilder b = new StringBuilder();
        b.append(n.contextId == null ? "" : n.contextId).append('|');
        b.append(n.type == null ? "" : n.type).append('|');
        b.append(n.title == null ? "" : n.title).append('|');
        b.append(n.message == null ? "" : n.message);
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
