package de.bund.zrb.ui.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.service.RecorderTabUi;
import de.bund.zrb.service.RecordingSession;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.WDEventContextExtractor;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Dünner Adapter zwischen BiDi-Event-Wiring (WDUiAppender), RecordingSession und Event-Monitor-UI.
 * Keine eigene Logik außer: anhängen/abklemmen, Flags durchreichen, Events schön darstellen.
 */
public final class RecorderEventController {

    private volatile String userContextFilter;

    private final RecorderTabUi ui;
    private final RecordingSession session;

    // aktive Appender (Page/Context)
    private final List<WDUiAppender> active = new CopyOnWriteArrayList<>();

    // pretty JSON for tooltips
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Δt seit letztem Event (nur UI, keine Logik)
    private volatile long lastEventTs = System.currentTimeMillis();

    // ein gemeinsamer Sink für alle Appender (EventName + RawPayload)
    private final BiConsumer<String, Object> sink = new BiConsumer<String, Object>() {
        @Override public void accept(String eventName, Object payload) {
            if (eventName == null) return;

            String ucId = WDEventContextExtractor.extractUserContextId(eventName, payload);

            // Wenn userContextFilter gesetzt ist, alles andere ignorieren
            if (userContextFilter != null) {
                if (ucId != null && !userContextFilter.equals(ucId)) return;
            }

            // 1) immer mitschreiben (für Timing/Analyse)
            session.recordRawEvent(eventName, payload);

            // 2) UI-Eintrag bauen (HTML-Label mit Details + Tooltip)
            final JLabel label = buildEventLabel(eventName, payload);
            label.putClientProperty("eventName", eventName);

            // 3) ins Event-Monitor-Fenster des aktuellen Users einhängen (auf dem EDT!)
            final UserRegistry.User user = resolveUser();
            if (user == null) return;

            SwingUtilities.invokeLater(() -> {
                EventMonitorWindow win = EventMonitorManager.getOrCreate(user);
                if (win != null) {
                    win.appendEvent(eventName, label);
                }
            });
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
        // Optional: Flags zusätzlich im Fenster spiegeln, falls gewünscht
        // UserRegistry.User user = resolveUser();
        // if (user != null) {
        //   EventMonitorWindow win = EventMonitorManager.getOrCreate(user);
        //   if (win != null) win.setFlags(new EnumMap<>(flags), null);
        // }
    }

    /** Trennt alle Event-Wirings. */
    public synchronized void stop() {
        for (WDUiAppender a : active) {
            try { a.detachAll(); } catch (Throwable ignore) {}
        }
        active.clear();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Darstellung/Formatter
    // -----------------------------------------------------------------------------------------------------------------

    private JLabel buildEventLabel(String name, Object payload) {
        long now = System.currentTimeMillis();
        long delta = now - lastEventTs;
        lastEventTs = now;

        String html = renderHtmlLine(name, payload, delta);
        JLabel lbl = new JLabel(html);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 12));

        // Tooltip mit pretty JSON (falls möglich)
        try {
            String tip = (payload == null) ? "(null)" : GSON.toJson(payload);
            if (tip.length() > 4000) tip = tip.substring(0, 4000) + " …";
            lbl.setToolTipText("<html><pre style='margin:4px 6px;'>" +
                    escapeHtml(tip) + "</pre></html>");
        } catch (Throwable ignore) {
            if (payload != null) lbl.setToolTipText(String.valueOf(payload));
        }
        return lbl;
    }

    /** Baut eine kompakte HTML-Zeile pro Event. */
    @SuppressWarnings("unchecked")
    private String renderHtmlLine(String name, Object payload, long deltaMs) {
        String tag = shortName(name);
        StringBuilder sb = new StringBuilder(128);
        sb.append("<html>");

        // Haupt-Tag
        sb.append("<b>").append(escapeHtml(tag)).append("</b>");

        Map<?,?> m = (payload instanceof Map) ? (Map<?,?>) payload : null;

        // Network: request/response/done
        if (name.startsWith("network.beforeRequestSent")) {
            String method = deepStr(m, "request", "method");
            String url    = deepStr(m, "request", "url");
            sb.append("&nbsp;&nbsp;<span style='color:#444;'>")
                    .append(escapeHtml(nullTo(method, "GET"))).append(" ")
                    .append(escapeHtml(shortUrl(url))).append("</span>");
        } else if (name.startsWith("network.responseStarted") || name.startsWith("network.responseCompleted")) {
            String method = deepStr(m, "request", "method");
            String url    = deepStr(m, "request", "url");
            String status = deepStr(m, "response", "status");
            String mime   = firstNonNull(
                    header(m, "content-type"),
                    deepStr(m, "response", "mimeType")
            );
            String size = humanSize(
                    deepLong(m, "response", "encodedDataLength"),
                    deepLong(m, "response", "bytesReceived"),
                    -1L
            );

            sb.append("&nbsp;&nbsp;<span style='color:#444;'>")
                    .append(escapeHtml(nullTo(method, ""))).append(" ")
                    .append(escapeHtml(shortUrl(url))).append("</span>");

            sb.append("&nbsp;&nbsp;").append(colorStatus(status));

            if (mime != null) {
                sb.append("&nbsp;<span style='color:#666;'>")
                        .append(escapeHtml(mime)).append("</span>");
            }
            if (size != null) {
                sb.append("&nbsp;<span style='color:#666;'>")
                        .append(size).append("</span>");
            }
        }
        // Console
        else if (name.startsWith("log.entryAdded")) {
            String level = deepStr(m, "level");
            String text  = firstNonNull(deepStr(m, "text"), deepStr(m, "args", "0", "value"));
            if (level != null) {
                sb.append("&nbsp;&nbsp;<span style='color:#666;'>")
                        .append("console.").append(escapeHtml(level)).append("</span>");
            }
            if (text != null) {
                sb.append("&nbsp;&nbsp;").append(escapeHtml(ellipsize(text, 200)));
            }
        }
        // BrowsingContext / Navigation / DOM
        else if (name.startsWith("browsingContext.") || name.startsWith("log.")) {
            String url = deepStr(m, "url");
            if (url != null) {
                sb.append("&nbsp;&nbsp;<span style='color:#444;'>")
                        .append(escapeHtml(shortUrl(url))).append("</span>");
            }
        }
        // Sonstige (Channels/Custom)
        else {
            String type = deepStr(m, "type");
            String url  = deepStr(m, "url");
            String vis  = deepStr(m, "visibility");
            String msg  = deepStr(m, "message");

            if (type != null) {
                sb.append("&nbsp;&nbsp;<span style='color:#666;'>")
                        .append(escapeHtml(type)).append("</span>");
            }
            if (url != null) {
                sb.append("&nbsp;&nbsp;<span style='color:#444;'>")
                        .append(escapeHtml(shortUrl(url))).append("</span>");
            }
            if (vis != null) {
                sb.append("&nbsp;&nbsp;<span style='color:#666;'>")
                        .append("vis=").append(escapeHtml(vis)).append("</span>");
            }
            if (msg != null) {
                sb.append("&nbsp;&nbsp;").append(escapeHtml(ellipsize(msg, 160)));
            }
        }

        // am Ende: Δt
        sb.append("&nbsp;&nbsp;<span style='color:#999;'>")
                .append("+").append(deltaMs).append("ms")
                .append("</span>");

        sb.append("</html>");
        return sb.toString();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Kleine Hilfen
    // -----------------------------------------------------------------------------------------------------------------

    private static String colorStatus(String status) {
        if (status == null) return "";
        String color = "#888";
        try {
            int s = Integer.parseInt(status);
            if      (s >= 200 && s < 300) color = "#2e7d32"; // grün
            else if (s >= 300 && s < 400) color = "#ef6c00"; // orange
            else if (s >= 400)           color = "#c62828"; // rot
        } catch (Exception ignore) {}
        return "<span style='color:" + color + ";font-weight:bold;'>" + status + "</span>";
    }

    private static String humanSize(long... candidates) {
        long v = -1;
        for (long c : candidates) if (c > 0) { v = c; break; }
        if (v <= 0) return null;
        if (v < 1024) return v + " B";
        double kb = v / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.0f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    private static String header(Map<?,?> m, String name) {
        if (m == null || name == null) return null;
        Object h = deep(m, "response", "headers");
        if (!(h instanceof Map)) return null;
        Object v = ((Map<?,?>) h).get(name);
        return (v == null) ? null : String.valueOf(v);
    }

    private static Object deep(Map<?,?> m, String... path) {
        Object cur = m;
        for (String k : path) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(k);
            if (cur == null) return null;
        }
        return cur;
    }

    private static String deepStr(Map<?,?> m, String... path) {
        Object v = deep(m, path);
        return (v == null) ? null : String.valueOf(v);
    }

    private static long deepLong(Map<?,?> m, String... path) {
        Object v = deep(m, path);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return (v == null) ? -1L : Long.parseLong(String.valueOf(v)); }
        catch (Exception e) { return -1L; }
    }

    private static String shortUrl(String url) {
        if (url == null) return null;
        int scheme = url.indexOf("://");
        if (scheme >= 0) {
            int slash = url.indexOf('/', scheme + 3);
            if (slash > 0 && slash < url.length()) return url.substring(slash);
            return "/";
        }
        return url;
    }

    private static String ellipsize(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String nullTo(String v, String def) { return (v == null || v.isEmpty()) ? def : v; }

    private static String firstNonNull(String... vs) {
        if (vs == null) return null;
        for (String v : vs) if (v != null && !v.isEmpty()) return v;
        return null;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String shortName(String name) {
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

    // -------- intern --------

    private UserRegistry.User resolveUser() {
        String name = (ui != null) ? ui.getUsername() : null;
        if (name == null) return null;
        return UserRegistry.getInstance().getUser(name);
    }

    public RecorderEventController setUserContextFilter(String userContextId) {
        this.userContextFilter = userContextId;
        return this;
    }
}
