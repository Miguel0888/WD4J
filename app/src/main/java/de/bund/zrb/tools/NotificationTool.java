package de.bund.zrb.tools;

import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zentrales Tool für PrimeFaces-Growl-Expectations.
 * - Blockierendes Warten mit Timeout.
 * - Regex-Matching (Title/Message).
 * - Treffer werden für Popups "verbraucht" (kein doppeltes Swing-Dialog).
 */
public class NotificationTool extends AbstractUserTool {

    private final BrowserService browserService;

    public NotificationTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    /** Await auf konkrete Severity + Regex für Title/Message. */
    public GrowlNotification await(String severity/*INFO|WARN|ERROR|FATAL*/,
                                   String titleRegex,
                                   String messageRegex,
                                   long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        NotificationService svc = serviceForActivePage();
        return callAwaitWrappingTimeout(svc, severity, titleRegex, messageRegex, timeoutMs);
    }

    /** Await nur auf Message-Regex (beliebige Severity & Title). */
    public GrowlNotification awaitMessage(String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        NotificationService svc = serviceForActivePage();
        return callAwaitWrappingTimeout(svc, null, null, messageRegex, timeoutMs);
    }

    /**
     * Await auf Message-Regex und ALLE Capture-Gruppen zurückgeben (1..n).
     * Gibt eine leere Liste zurück, wenn kein Capture existiert, aber Match stattfand.
     */
    public List<String> awaitAndExtractAllGroups(String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        NotificationService svc = serviceForActivePage();
        GrowlNotification n = callAwaitWrappingTimeout(svc, null, null, messageRegex, timeoutMs);

        String msg = (n == null ? null : n.message);
        if (msg == null) return new ArrayList<String>(0);

        Pattern p;
        try {
            p = Pattern.compile(messageRegex);
        } catch (Exception ex) {
            throw new ExecutionException("Invalid regex: " + messageRegex, ex);
        }

        Matcher m = p.matcher(msg);
        if (!m.find()) {
            // Should not happen because service awaited the same regex; treat defensively.
            return new ArrayList<String>(0);
        }

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

    /**
     * (Kompatibilität) Await auf Message-Regex und die i-te Capture-Group zurückgeben.
     * Intern auf awaitAndExtractAllGroups(...) aufgebaut.
     */
    public String awaitAndExtractGroup(String messageRegex, int groupIndex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        List<String> groups = awaitAndExtractAllGroups(messageRegex, timeoutMs);
        if (groups == null || groups.isEmpty()) return null;

        // groupIndex ist 1-basiert (wie üblich in Regex-Denke).
        int idx = groupIndex - 1;
        if (idx < 0 || idx >= groups.size()) return null;
        return groups.get(idx);
    }

    // --- intern ---

    /** Resolve NotificationService anhand aktiver Page (wie bisher). */
    private NotificationService serviceForActivePage() {
        PageImpl active = (PageImpl) ((BrowserServiceImpl) browserService).getBrowser().getActivePage();
        if (active == null) {
            throw new IllegalStateException("Keine aktive Page verfügbar – kann NotificationService nicht auflösen.");
        }
        return NotificationService.getInstance(active);
    }

    /** Map NotificationService.TimeoutException → java.util.concurrent.TimeoutException. */
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
}
