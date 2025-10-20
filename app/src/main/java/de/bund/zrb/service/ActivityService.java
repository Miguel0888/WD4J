package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.*;
import de.bund.zrb.support.ScriptHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Push-only ActivityService (wie NotificationService), ohne WDTarget-Instanziierung. */
public final class ActivityService {

    private static final String CHANNEL = "activity-events-channel";
    private static final String SCRIPT_PATH = "scripts/activityMonitor.js";

    private static final Map<BrowserImpl, ActivityService> INSTANCES =
            new WeakHashMap<>();
    private static final Set<BrowserImpl> PRELOAD_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static final class Snapshot {
        public final long actionSeq, lastActionTs, lastChangeTs;
        public final long inflightXHR, inflightFetch, pfQueueDepth;
        public final long lastDomContentLoaded, lastLoad;

        Snapshot(long actionSeq, long lastActionTs, long lastChangeTs,
                 long inflightXHR, long inflightFetch, long pfQueueDepth,
                 long lastDomContentLoaded, long lastLoad) {
            this.actionSeq = actionSeq;
            this.lastActionTs = lastActionTs;
            this.lastChangeTs = lastChangeTs;
            this.inflightXHR = inflightXHR;
            this.inflightFetch = inflightFetch;
            this.pfQueueDepth = pfQueueDepth;
            this.lastDomContentLoaded = lastDomContentLoaded;
            this.lastLoad = lastLoad;
        }
    }

    public enum ResultType { NAV_OR_LOAD, IDLE, NO_EFFECT, TIMEOUT }
    public static final class AwaitResult {
        public final ResultType type; public final String details;
        public AwaitResult(ResultType t, String d){ this.type=t; this.details=d; }
        public String toString(){ return type+(details!=null?(":"+details):""); }
    }

    private final BrowserImpl browser;
    private final AtomicReference<Snapshot> last = new AtomicReference<>(null);

    private ActivityService(BrowserImpl browser) {
        this.browser = browser;
        ensurePreloadInstalled(browser);
        this.browser.onMessage(ev -> {
            try {
                if (!(ev instanceof WDScriptEvent.MessageWD)) return;
                WDScriptEvent.MessageWD m = (WDScriptEvent.MessageWD) ev;
                if (m.getParams() == null) return;
                String ch = null;
                try {
                    ch = m.getParams().getChannel() == null ? null : m.getParams().getChannel().value();
                } catch (Throwable ignore) {}
                if (!CHANNEL.equals(ch)) return;

                Snapshot s = parseSnapshot(m);
                if (s != null) last.set(s);
            } catch (Throwable ignore) { }
        });
    }

    public static synchronized ActivityService getInstance(BrowserImpl browser) {
        ActivityService svc = INSTANCES.get(browser);
        if (svc == null) {
            svc = new ActivityService(browser);
            INSTANCES.put(browser, svc);
        }
        return svc;
    }
    public static ActivityService getInstance(PageImpl page) {
        return getInstance(page.getBrowser());
    }

    private static synchronized void ensurePreloadInstalled(BrowserImpl browser) {
        if (browser == null) return;
        if (PRELOAD_INSTALLED.contains(browser)) return;
        try {
            String source = ScriptHelper.loadScript(SCRIPT_PATH);
            browser.getScriptManager().addPreloadScript(
                    source,
                    java.util.Collections.singletonList(
                            new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(CHANNEL)))
                    )
            );
            PRELOAD_INSTALLED.add(browser);
        } catch (Throwable t) {
            System.err.println("[ActivityService] Failed to install preload: " + t.getMessage());
        }
    }

    /** Hauptlogik: nach Aktion warten, bis stabil – nur anhand letzter Push-Snapshots. */
    public AwaitResult awaitAfterAction(long maxWaitMs, long idleQuietMs) throws InterruptedException {
        if (maxWaitMs <= 0) maxWaitMs = 2500L;
        if (idleQuietMs <= 0) idleQuietMs = 200L;

        long start = System.currentTimeMillis();

        // baseline
        Snapshot s0 = spinUntilNonNull(500);
        if (s0 == null) return new AwaitResult(ResultType.TIMEOUT, "no snapshot");

        long quietStart = -1;
        while (true) {
            Snapshot s = last.get();
            if (s == null) {
                if (System.currentTimeMillis() - start > maxWaitMs)
                    return new AwaitResult(ResultType.TIMEOUT, "no snapshot update");
                Thread.sleep(25);
                continue;
            }

            // 1) Navigation- oder Load-Marker gesehen?
            if (s.lastDomContentLoaded > s0.lastDomContentLoaded || s.lastLoad > s0.lastLoad) {
                if (s.inflightXHR == 0 && s.inflightFetch == 0 && s.pfQueueDepth == 0) {
                    return new AwaitResult(ResultType.NAV_OR_LOAD, "domContentLoaded/load observed");
                }
            }

            // 2) AJAX/PF idle + kurze Ruhe
            boolean idle = (s.inflightXHR == 0 && s.inflightFetch == 0 && s.pfQueueDepth == 0);
            if (idle) {
                if (quietStart < 0) quietStart = System.currentTimeMillis();
                if (System.currentTimeMillis() - quietStart >= idleQuietMs) {
                    return new AwaitResult(ResultType.IDLE, "ajax=0 & pf=0 & quiet");
                }
            } else {
                quietStart = -1;
            }

            // 3) NO_EFFECT: keinerlei Änderung vs. baseline
            if (System.currentTimeMillis() - start >= 250) {
                boolean noNav = (s.lastDomContentLoaded <= s0.lastDomContentLoaded && s.lastLoad <= s0.lastLoad);
                boolean noNet = idle;
                boolean noDom = (s.lastChangeTs <= s0.lastChangeTs);
                if (noNav && noNet && noDom) {
                    return new AwaitResult(ResultType.NO_EFFECT, "no nav/net/dom-change");
                }
            }

            if (System.currentTimeMillis() - start > maxWaitMs) {
                return new AwaitResult(ResultType.TIMEOUT, "not settled within " + maxWaitMs + " ms");
            }

            Thread.sleep(50);
        }
    }

    private Snapshot spinUntilNonNull(long ms) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (true) {
            Snapshot s = last.get();
            if (s != null) return s;
            if (System.currentTimeMillis() - start > ms) return null;
            Thread.sleep(25);
        }
    }

    // --- Mapping aus Message (wie bei NotificationService) ---
    private static ActivityService.Snapshot parseSnapshot(WDScriptEvent.MessageWD message) {
        WDRemoteValue dataVal = message.getParams().getData();
        if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return null;
        WDRemoteValue.ObjectRemoteValue env = (WDRemoteValue.ObjectRemoteValue) dataVal;

        String envelopeType = null;
        WDRemoteValue.ObjectRemoteValue dataObj = null;

        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : env.getValue().entrySet()) {
            if (!(e.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;
            String key = ((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue();
            WDRemoteValue val = e.getValue();
            if ("type".equals(key) && val instanceof WDPrimitiveProtocolValue.StringValue) {
                envelopeType = ((WDPrimitiveProtocolValue.StringValue) val).getValue();
            } else if ("data".equals(key) && val instanceof WDRemoteValue.ObjectRemoteValue) {
                dataObj = (WDRemoteValue.ObjectRemoteValue) val;
            }
        }
        if (!"activity-event".equals(envelopeType) || dataObj == null) return null;

        Map<String,Object> m = toPlainObject(dataObj);

        return new Snapshot(
                asLong(m.get("actionSeq")),
                asLong(m.get("lastActionTs")),
                asLong(m.get("lastChangeTs")),
                asLong(m.get("inflightXHR")),
                asLong(m.get("inflightFetch")),
                asLong(m.get("pfQueueDepth")),
                asLong(m.get("lastDomContentLoaded")),
                asLong(m.get("lastLoad"))
        );
    }

    private static long asLong(Object o){
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number)o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch(Exception e){ return 0L; }
    }

    /* ===== Helpers: WDRemoteValue -> Plain Java ===== */

    @SuppressWarnings("unchecked")
    private static Map<String,Object> toPlainObject(WDRemoteValue.ObjectRemoteValue obj) {
        Map<String,Object> out = new LinkedHashMap<>();
        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : obj.getValue().entrySet()) {
            if (!(e.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;
            String k = ((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue();
            out.put(k, toJava(e.getValue()));
        }
        return out;
    }

    private static Object toJava(WDRemoteValue v) {
        if (v == null) return null;

        // Primitives
        if (v instanceof WDPrimitiveProtocolValue.StringValue)
            return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.NumberValue)
            return ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue)
            return ((WDPrimitiveProtocolValue.BooleanValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.NullValue)
            return null;

        // Arrays
        if (v instanceof WDRemoteValue.ArrayRemoteValue) {
            List<Object> list = new ArrayList<>();
            for (WDRemoteValue it : ((WDRemoteValue.ArrayRemoteValue) v).getValue()) {
                list.add(toJava(it));
            }
            return list;
        }

        // Objects
        if (v instanceof WDRemoteValue.ObjectRemoteValue) {
            return toPlainObject((WDRemoteValue.ObjectRemoteValue) v);
        }

        // Knoten/Referenzen (NodeRemoteValue, RemoteReference etc.): hier nicht benötigt → als String kennzeichnen oder null
        if (v instanceof WDRemoteValue.NodeRemoteValue) {
            return "[Node]";
        }

        // Fallback
        return String.valueOf(v);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // WAIT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // A) Aktion markieren (im Seitenkontext)
    public static void markAction(Page page) {
        try {
            page.evaluate("() => { if (window.__zrbMarkAction) window.__zrbMarkAction(); }");
        } catch (Throwable ignore) {
            // Falls Preload noch nicht injectiert sein sollte: nicht hart fehlschlagen
        }
    }

    // B) Nachlauf-Settling (wartet robust auf Idle/Nav)
    public static boolean awaitSettle(PageImpl page, double actionTimeoutMs) {
        try {
            // sinnvolle Defaults
            long maxWaitMs   = (long) (actionTimeoutMs > 0 ? actionTimeoutMs : 5000); // Click etc.
            long idleQuietMs = 180; // 120–200ms funktionieren meist gut

            ActivityService.AwaitResult r =
                    ActivityService.getInstance(page).awaitAfterAction(maxWaitMs, idleQuietMs);

            switch (r.type) {
                case NAV_OR_LOAD:
                case IDLE:
                    return true;
                case NO_EFFECT:
                    // Aktion hatte keine beobachtbare Wirkung – je nach Policy kann das ok oder fail sein.
                    // Hier: als "weich ok" behandeln; loggst du ohnehin im StepLog.
                    return true;
                case TIMEOUT:
                default:
                    return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    // C) Komfort: Markieren → Aktion → Settlen (eine Zeile in den Cases)
    public static boolean doWithSettling(PageImpl page, double timeoutMs, Runnable action) {
        markAction(page);
        action.run();
        return awaitSettle(page, timeoutMs);
    }



}
