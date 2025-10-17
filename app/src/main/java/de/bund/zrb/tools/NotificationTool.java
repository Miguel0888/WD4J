package de.bund.zrb.tools;

import de.bund.zrb.PageImpl;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.NotificationService;
import de.bund.zrb.dto.GrowlNotification;

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
        return svc.await(severity, titleRegex, messageRegex, timeoutMs);
    }

    /** Await nur auf Message-Regex (beliebige Severity & Title). */
    public GrowlNotification awaitMessage(String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        NotificationService svc = serviceForActivePage();
        return svc.await(null, null, messageRegex, timeoutMs);
    }

    /** Await auf Message-Regex und 1. Capture-Group zurückgeben (oder null). */
    public String awaitAndExtractGroup(String messageRegex, int groupIndex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        NotificationService svc = serviceForActivePage();
        GrowlNotification n = svc.await(null, null, messageRegex, timeoutMs);

        if (n == null || n.message == null) return null;
        Matcher m = Pattern.compile(messageRegex).matcher(n.message);
        return m.find() ? safeGroup(m, groupIndex) : null;
    }

    /** Bequeme Helfer für gängige Severities */
    public GrowlNotification awaitInfo(String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return await("INFO", titleRegex, messageRegex, timeoutMs);
    }
    public GrowlNotification awaitWarn(String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return await("WARN", titleRegex, messageRegex, timeoutMs);
    }
    public GrowlNotification awaitError(String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return await("ERROR", titleRegex, messageRegex, timeoutMs);
    }
    public GrowlNotification awaitFatal(String titleRegex, String messageRegex, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return await("FATAL", titleRegex, messageRegex, timeoutMs);
    }

    // --- intern ---

    private NotificationService serviceForActivePage() {
        // Wir nehmen bewusst die aktive Page als Key (wie bei deiner bisherigen Praxis).
        PageImpl active = (PageImpl) ((BrowserServiceImpl) browserService).getBrowser().getActivePage();
        if (active == null) {
            throw new IllegalStateException("Keine aktive Page verfügbar – kann NotificationService nicht auflösen.");
        }
        return NotificationService.getInstance(active);
    }

    private static String safeGroup(Matcher m, int idx) {
        try {
            return m.group(idx);
        } catch (Exception ignore) {
            return null;
        }
    }
}
