package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Dünner Adapter zwischen BiDi-Event-Wiring (WDUiAppender), RecordingSession und Recorder-UI.
 * Keine eigene Logik außer: anhängen/abklemmen, Flags durchreichen, Events schön darstellen.
 */
public final class RecorderEventController {

    private volatile String userContextFilter;

    private final RecorderTabUi ui;
    private final RecordingSession session;

    // aktive Appender (Page/Context)
    private final List<WDUiAppender> active = new CopyOnWriteArrayList<>();

    // ein gemeinsamer Sink für alle Appender (EventName + RawPayload)
    private final BiConsumer<String, Object> sink = new BiConsumer<String, Object>() {
        @Override public void accept(String eventName, Object payload) {
            if (eventName == null) return;

            // Pseudocode im Controller-Listener:
            String ctxId  = WDEventContextExtractor.extractContextId(eventName, payload); // ToDo: Kann man meist nicht extrahieren, muss über page bezogen werden
            String ucId   = WDEventContextExtractor.extractUserContextId(eventName, payload);

            // Wenn du eine Context→UserContext-Map führst, kannst du ucId ggf. aus ctxId ableiten.
            // Für den schnellen Fix reicht: wenn userContextFilter gesetzt ist, alles andere ignorieren
            if (userContextFilter != null) {
                if (ucId != null && !userContextFilter.equals(ucId)) return; // falscher User → ignorieren
                // optionaler Fallback: wenn ucId fehlt, aber du ctx→uc gemappt hast, dort prüfen
            }

            // 1) immer mitschreiben (für Timing/Analyse)
            session.recordRawEvent(eventName, payload);

            // 2) UI Eintrag bauen
            String line = summarize(eventName, payload);
            JLabel label = new JLabel(line);
            // Event-Typ für UI-Filter merken
            label.putClientProperty("eventName", eventName);

            // 3) in den Recorder unten einhängen
            ui.appendEvent(eventName, label);
        }
    };

    public RecorderEventController(RecorderTabUi ui, RecordingSession session) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.session = Objects.requireNonNull(session, "session");
    }

    /** Startet Event-Wiring für einen BrowserContext. */
    public synchronized void start(BrowserContext context) {
        stop(); // sicherstellen, dass nix doppelt hängt
        if (context == null) return;

        // mit aktuellen Flags und Default-Wiring verbinden
        WDUiAppender a = WDUiAppender.attachToContext(
                context,
                sink,
                WDEventWiringConfig.defaults(),
                session.getEventFlags()
        );
        active.add(a);
    }

    /** Startet Event-Wiring für eine Page. */
    public synchronized void start(Page page) {
        stop();
        if (page == null) return;

        WDUiAppender a = WDUiAppender.attachToPage(
                page,
                sink,
                WDEventWiringConfig.defaults(),
                session.getEventFlags()
        );
        active.add(a);
    }

    /** Aktualisiert die Enable-Flags live auf allen aktiven Appendern. */
    public synchronized void updateFlags(Map<WDEventNames, Boolean> flags) {
        if (flags == null) return;
        for (WDUiAppender a : active) {
            try { a.update(new EnumMap<WDEventNames, Boolean>(flags)); } catch (Throwable ignore) {}
        }
    }

    /** Trennt alle Event-Wirings. */
    public synchronized void stop() {
        for (WDUiAppender a : active) {
            try { a.detachAll(); } catch (Throwable ignore) {}
        }
        active.clear();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Kleine Hilfen, um die Meta-Zeile kompakt & nützlich zu machen
    // -----------------------------------------------------------------------------------------------------------------

    private static String summarize(String name, Object payload) {
        // Standard: nur Eventname
        String base = shortName(name);

        // Versuche, bei network-Events eine URL/Status kurz anzuhängen
        if (payload instanceof Map) {
            Map<?,?> m = (Map<?,?>) payload;

            if (name.startsWith("network.beforeRequestSent")) {
                String url = deepStr(m, "request", "url");
                if (url != null) return base + "  " + trimUrl(url);
            }
            if (name.startsWith("network.responseStarted")) {
                String url = deepStr(m, "request", "url");
                String status = deepStr(m, "response", "status");
                if (url != null) return base + "  " + trimUrl(url) + (status != null ? "  [" + status + "]" : "");
            }
            if (name.startsWith("network.responseCompleted")) {
                String url = deepStr(m, "request", "url");
                String status = deepStr(m, "response", "status");
                if (url != null) return base + "  " + trimUrl(url) + (status != null ? "  [" + status + "]" : "");
            }
            if (name.startsWith("log.entryAdded")) {
                String text = deepStr(m, "text");
                if (text == null) text = deepStr(m, "args", "0", "value");
                if (text != null) return base + "  " + ellipsize(text, 160);
            }
            if (name.startsWith("browsingContext.")) {
                String url = deepStr(m, "url");
                if (url != null) return base + "  " + trimUrl(url);
            }
        }
        return base;
    }

    private static String shortName(String name) {
        // Kürzere Labels wie im UI (request/response/done/…)
        WDEventNames ev = WDEventNames.fromName(name);
        if (ev == null) return name;
        switch (ev) {
            case BEFORE_REQUEST_SENT: return "request";
            case RESPONSE_STARTED:    return "response";
            case RESPONSE_COMPLETED:  return "done";
            case FETCH_ERROR:         return "error";
            case DOM_CONTENT_LOADED:  return "dom";
            case LOAD:                return "load";
            case ENTRY_ADDED:         return "console";
            case CONTEXT_CREATED:     return "ctx+";
            case CONTEXT_DESTROYED:   return "ctx-";
            case FRAGMENT_NAVIGATED:  return "hash";
            case NAVIGATION_STARTED:  return "nav";
            default: return ev.name().toLowerCase().replace('_',' ');
        }
    }

    // --- kleine Map-Utils (tolerant) ---
    @SuppressWarnings("unchecked")
    private static String deepStr(Map<?,?> m, String... path) {
        Object cur = m;
        for (String k : path) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(k);
            if (cur == null) return null;
        }
        return String.valueOf(cur);
    }

    private static String trimUrl(String url) {
        if (url == null) return null;
        // nur Pfad + Query zeigen, Host weglassen (knapper)
        try {
            int idx = url.indexOf("://");
            if (idx > 0) {
                int slash = url.indexOf('/', idx + 3);
                if (slash > 0 && slash < url.length()-1) {
                    return url.substring(slash);
                }
            }
        } catch (Throwable ignore) {}
        return url;
    }

    private static String ellipsize(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    public RecorderEventController setUserContextFilter(String userContextId) {
        this.userContextFilter = userContextId;
        return this;
    }
}
