package de.bund.zrb.tools;

import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.NotificationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zentrales Tool für PrimeFaces-Growl-Expectations.
 * Stellt Built-ins für Expressions bereit:
 *  - AwaitNotification(messageRegex[, severity][, titleRegex][, timeoutMs])
 *  - AwaitNotificationGroups(messageRegex[, severity][, titleRegex][, timeoutMs])
 *  - FindOrAwaitNotification(messageRegex[, severity][, titleRegex][, timeoutMs])
 *  - FindOrAwaitNotificationGroups(messageRegex[, severity][, titleRegex][, timeoutMs])
 *  - ClearNotificationHistory()
 */
public class NotificationTool extends AbstractUserTool implements BuiltinTool {

    private static final long DEFAULT_TIMEOUT_MS = 30_000L;

    private final BrowserService browserService;

    public NotificationTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    // ====================================================================================
    // Öffentliche API – fachlich (wird sowohl von UI als auch Built-ins genutzt)
    // ====================================================================================

    /** Warte auf Severity/Title/Message mit Timeout. */
    public GrowlNotification await(String severity,
                                   String titleRegex,
                                   String messageRegex,
                                   long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        NotificationService svc = serviceForActivePage();
        return callAwaitWrappingTimeout(svc, severity, titleRegex, messageRegex, timeoutMs);
    }

    /** Warte nur auf Message-Regex (beliebige Severity & Title). */
    public GrowlNotification awaitMessage(String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        NotificationService svc = serviceForActivePage();
        return callAwaitWrappingTimeout(svc, null, null, messageRegex, timeoutMs);
    }

    /** Warte auf Message-Regex und gib ALLE Capture-Gruppen (1..n) zurück. */
    public List<String> awaitAndExtractAllGroups(String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        GrowlNotification n = awaitMessage(messageRegex, timeoutMs);
        return extractGroupsFromMessage(n == null ? null : n.message, messageRegex);
    }

    /** Warte und gib die i-te Capture-Group (1-basiert) zurück. */
    public String awaitAndExtractGroup(String messageRegex, int groupIndex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        List<String> groups = awaitAndExtractAllGroups(messageRegex, timeoutMs);
        if (groups == null || groups.isEmpty()) return null;
        int idx = groupIndex - 1;
        return (idx >= 0 && idx < groups.size()) ? groups.get(idx) : null;
    }

    /** History löschen (für aktive Page). */
    public void clearHistory() {
        NotificationService svc = serviceForActivePage();
        svc.clearHistory();
    }

    // ====================================================================================
    // BuiltinTool – Expression-Funktionen
    // ====================================================================================

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        // --------------------------------------------------------------------
        // 1) Nur warten – Message (pflicht), dann Severity?, TitleRegex?, Timeout?
        // --------------------------------------------------------------------

        // AwaitNotification(...) -> String (message)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "AwaitNotification",
                        "Warte auf eine Notification, die auf messageRegex matcht. Optional filtere Severity (INFO|WARN|ERROR|FATAL oder ANY), "
                                + "optional Title-Regex und Timeout (ms). Gibt die Message zurück.",
                        ToolExpressionFunction.params("messageRegex", "severity?", "titleRegex?", "timeoutMs?"),
                        Arrays.asList(
                                "Regex für den Nachrichtentext (required).",
                                "Schweregrad: INFO|WARN|ERROR|FATAL|ANY (optional, Default ANY).",
                                "Regex für den Titel (optional).",
                                "Timeout in Millisekunden (optional, Default 30000)."
                        )
                ),
                1, 4,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        Parsed p = parseArgs(args);
                        GrowlNotification n = await(p.severity, p.titleRx, p.messageRx, p.timeoutMs);
                        return n == null || n.message == null ? "" : n.message;
                    }
                }
        ));

        // AwaitNotificationGroups(...) -> String (Gruppen)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "AwaitNotificationGroups",
                        "Warte auf Notification per Regex. Gibt bei genau 1 Capture-Group nur deren Wert zurück, "
                                + "sonst alle Gruppen als „; “-konkatenierten String.",
                        ToolExpressionFunction.params("messageRegex", "severity?", "titleRegex?", "timeoutMs?"),
                        Arrays.asList(
                                "Regex mit Capture-Gruppen für den Nachrichtentext (required).",
                                "Schweregrad: INFO|WARN|ERROR|FATAL|ANY (optional, Default ANY).",
                                "Regex für den Titel (optional).",
                                "Timeout in Millisekunden (optional, Default 30000)."
                        )
                ),
                1, 4,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        Parsed p = parseArgs(args);
                        GrowlNotification n = await(p.severity, p.titleRx, p.messageRx, p.timeoutMs);
                        List<String> groups = extractGroupsFromMessage(n == null ? null : n.message, p.messageRx);
                        return formatGroups(groups);
                    }
                }
        ));

        // --------------------------------------------------------------------
        // 2) History-first, danach warten (gleiche Parameter)
        // --------------------------------------------------------------------

        // FindOrAwaitNotification(...) -> String (message)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "FindOrAwaitNotification",
                        "Durchsuche zuerst die History nach einer passenden Notification. "
                                + "Wenn keine gefunden wird, warte (wie AwaitNotification). Gibt die Message zurück.",
                        ToolExpressionFunction.params("messageRegex", "severity?", "titleRegex?", "timeoutMs?"),
                        Arrays.asList(
                                "Regex für den Nachrichtentext (required).",
                                "Schweregrad: INFO|WARN|ERROR|FATAL|ANY (optional, Default ANY).",
                                "Regex für den Titel (optional).",
                                "Timeout in Millisekunden (optional, Default 30000)."
                        )
                ),
                1, 4,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        Parsed p = parseArgs(args);
                        GrowlNotification hit = findInHistory(p.severity, p.titleRx, p.messageRx);
                        if (hit != null) {
                            return hit.message == null ? "" : hit.message;
                        }
                        GrowlNotification n = await(p.severity, p.titleRx, p.messageRx, p.timeoutMs);
                        return n == null || n.message == null ? "" : n.message;
                    }
                }
        ));

        // FindOrAwaitNotificationGroups(...) -> String (Gruppen)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "FindOrAwaitNotificationGroups",
                        "History-first: Suche in der History, sonst warte. "
                                + "Gibt bei genau 1 Capture-Group nur deren Wert aus, sonst „; “-konkateniert.",
                        ToolExpressionFunction.params("messageRegex", "severity?", "titleRegex?", "timeoutMs?"),
                        Arrays.asList(
                                "Regex mit Capture-Gruppen für den Nachrichtentext (required).",
                                "Schweregrad: INFO|WARN|ERROR|FATAL|ANY (optional, Default ANY).",
                                "Regex für den Titel (optional).",
                                "Timeout in Millisekunden (optional, Default 30000)."
                        )
                ),
                1, 4,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        Parsed p = parseArgs(args);
                        GrowlNotification hit = findInHistory(p.severity, p.titleRx, p.messageRx);
                        List<String> groups;
                        if (hit != null) {
                            groups = extractGroupsFromMessage(hit.message, p.messageRx);
                        } else {
                            GrowlNotification n = await(p.severity, p.titleRx, p.messageRx, p.timeoutMs);
                            groups = extractGroupsFromMessage(n == null ? null : n.message, p.messageRx);
                        }
                        return formatGroups(groups);
                    }
                }
        ));

        // --------------------------------------------------------------------
        // 3) History löschen
        // --------------------------------------------------------------------
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "ClearNotificationHistory",
                        "Lösche die Notification-History der aktiven Page.",
                        ToolExpressionFunction.params(), // keine Parameter
                        new ArrayList<String>(0)
                ),
                0, 0,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) {
                        clearHistory();
                        return "OK";
                    }
                }
        ));

        return list;
    }

    // ====================================================================================
    // intern – Parsing, Matching, Utilities
    // ====================================================================================

    /** Kapselt die optionalen Parameter. */
    private static final class Parsed {
        final String messageRx;
        final String severity;   // null oder NORMALISIERT (INFO/WARN/ERROR/FATAL) – ANY => null
        final String titleRx;
        final long timeoutMs;

        Parsed(String msg, String sev, String title, long to) {
            this.messageRx = msg;
            this.severity = sev;
            this.titleRx = title;
            this.timeoutMs = to;
        }
    }

    /** Parse args in der Reihenfolge: messageRegex, severity?, titleRegex?, timeoutMs?. */
    private Parsed parseArgs(List<String> args) {
        String msgRx = arg(args, 0);
        if (msgRx == null || msgRx.trim().length() == 0) {
            throw new IllegalArgumentException("messageRegex ist erforderlich.");
        }
        String sevIn = arg(args, 1);
        String sev = normalizeSeverity(sevIn); // ANY → null
        String titleRx = arg(args, 2);
        Long to = parseLongSafe(arg(args, 3));
        long timeout = (to == null ? DEFAULT_TIMEOUT_MS : Math.max(0L, to.longValue()));
        return new Parsed(msgRx, sev, titleRx, timeout);
    }

    private String arg(List<String> a, int idx) {
        return (a != null && a.size() > idx) ? emptyToNull(a.get(idx)) : null;
    }

    private String emptyToNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }

    private Long parseLongSafe(String s) {
        if (s == null) return null;
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    /** Severity normalisieren – ANY oder leer -> null (=beliebig). */
    private String normalizeSeverity(String s) {
        if (s == null) return null;
        String u = s.trim().toUpperCase();
        if (u.length() == 0 || "ANY".equals(u)) return null;
        // Erlaube nur bekannte Werte
        if ("INFO".equals(u) || "WARN".equals(u) || "ERROR".equals(u) || "FATAL".equals(u)) return u;
        // Fallback: behandle Unbekanntes als beliebig (null), um überraschende Fehler zu vermeiden
        return null;
    }

    /** History-Scan: erstes passendes Element zurückgeben (null, wenn keines). */
    private GrowlNotification findInHistory(String severity, String titleRegex, String messageRegex) throws ExecutionException {
        NotificationService svc = serviceForActivePage();
        Pattern msg = compileOrNull(messageRegex);
        Pattern ttl = compileOrNull(titleRegex);

        List<GrowlNotification> all = svc.getAll();
        for (int i = 0; i < all.size(); i++) {
            GrowlNotification n = all.get(i);
            if (!matches(n, severity, ttl, msg)) continue;
            return n;
        }
        return null;
    }

    /** Prüfe, ob Notification Kriterien erfüllt. */
    private boolean matches(GrowlNotification n, String severity, Pattern titleRx, Pattern messageRx) {
        if (n == null) return false;
        if (severity != null) {
            String t = n.type == null ? "" : n.type.toUpperCase();
            if (!severity.equals(t)) return false;
        }
        if (titleRx != null) {
            String t = n.title == null ? "" : n.title;
            if (!titleRx.matcher(t).find()) return false;
        }
        if (messageRx != null) {
            String m = n.message == null ? "" : n.message;
            if (!messageRx.matcher(m).find()) return false;
        }
        return true;
    }

    /** Gruppen aus message anhand des gleichen Regex extrahieren. */
    private List<String> extractGroupsFromMessage(String message, String messageRegex) throws ExecutionException {
        if (message == null) return new ArrayList<String>(0);
        Pattern p = compileOrFail(messageRegex);
        Matcher m = p.matcher(message);
        if (!m.find()) return new ArrayList<String>(0);

        int count = m.groupCount();
        List<String> groups = new ArrayList<String>(count);
        for (int i = 1; i <= count; i++) {
            try {
                groups.add(m.group(i));
            } catch (Exception ignore) {
                groups.add(null);
            }
        }
        return groups;
    }

    /** Format gemäß Anforderung: 1 Gruppe => nur diese; sonst „; “-konkateniert. */
    private String formatGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) return "";
        if (groups.size() == 1) {
            String g = groups.get(0);
            return g == null ? "" : g;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) sb.append("; ");
            String g = groups.get(i);
            sb.append(g == null ? "" : g);
        }
        return sb.toString();
    }

    // ====================================================================================
    // Service/Interop
    // ====================================================================================

    private NotificationService serviceForActivePage() {
        PageImpl active = (PageImpl) ((BrowserServiceImpl) browserService).getBrowser().getActivePage();
        if (active == null) {
            throw new IllegalStateException("Keine aktive Page verfügbar – kann NotificationService nicht auflösen.");
        }
        return NotificationService.getInstance(active);
    }

    private GrowlNotification callAwaitWrappingTimeout(NotificationService svc,
                                                       String severity,
                                                       String titleRegex,
                                                       String messageRegex,
                                                       long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        try {
            return svc.await(severity, titleRegex, messageRegex, timeoutMs);
        } catch (NotificationService.TimeoutException e) {
            TimeoutException te = new TimeoutException(e.getMessage());
            te.initCause(e);
            throw te;
        }
    }

    private Pattern compileOrNull(String regex) throws ExecutionException {
        if (regex == null || regex.trim().isEmpty()) return null;
        try { return Pattern.compile(regex); }
        catch (Exception ex) { throw new ExecutionException("Invalid regex: " + regex, ex); }
    }

    private Pattern compileOrFail(String regex) throws ExecutionException {
        if (regex == null || regex.trim().isEmpty()) {
            throw new ExecutionException("Invalid regex: <empty>", null);
        }
        try { return Pattern.compile(regex); }
        catch (Exception ex) { throw new ExecutionException("Invalid regex: " + regex, ex); }
    }
}
