package de.bund.zrb.util;

import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.NotificationService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Komfort-API für blockierendes Warten auf PrimeFaces-Growl-Notifications mit Regex-Matching.
 * Nutzt NotificationService.await(...) unter der Haube und markiert Treffer als "consumed",
 * damit der Swing-Popup-Helper sie nicht doppelt anzeigt.
 */
public final class GrowlAwait {

    private GrowlAwait() {}

    // ----------------------------- Public API (Convenience) -----------------------------

    /** Warte auf INFO, optional nach Kontext filtern, und match(e) per Regex auf Title/Message. */
    public static Result awaitInfo(NotificationService svc,
                                   String contextIdOrNull,
                                   String titleRegexOrNull,
                                   String messageRegexOrNull,
                                   long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return awaitType(svc, "INFO", contextIdOrNull, titleRegexOrNull, messageRegexOrNull, timeoutMs);
    }

    /** Warte auf WARN. */
    public static Result awaitWarn(NotificationService svc,
                                   String contextIdOrNull,
                                   String titleRegexOrNull,
                                   String messageRegexOrNull,
                                   long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return awaitType(svc, "WARN", contextIdOrNull, titleRegexOrNull, messageRegexOrNull, timeoutMs);
    }

    /** Warte auf ERROR oder FATAL. */
    public static Result awaitError(NotificationService svc,
                                    String contextIdOrNull,
                                    String titleRegexOrNull,
                                    String messageRegexOrNull,
                                    long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        return awaitAny(svc, contextIdOrNull, "(ERROR|FATAL)", titleRegexOrNull, messageRegexOrNull, timeoutMs);
    }

    /** Warte auf beliebige Severity (per Regex) – z. B. "(INFO|WARN)". */
    public static Result awaitAny(NotificationService svc,
                                  String contextIdOrNull,
                                  String typeRegexOrNull,
                                  String titleRegexOrNull,
                                  String messageRegexOrNull,
                                  long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        final Pattern pType   = compileOrNull(typeRegexOrNull);
        final Pattern pTitle  = compileOrNull(titleRegexOrNull);
        final Pattern pMsg    = compileOrNull(messageRegexOrNull);

        GrowlNotification n = svc.await(gn -> {
            if (contextIdOrNull != null && !contextIdOrNull.equals(gn.contextId)) return false;
            if (pType  != null && (gn.type == null   || !pType.matcher(gn.type).find())) return false;
            if (pTitle != null && (gn.title == null  || !pTitle.matcher(gn.title).find())) return false;
            if (pMsg   != null && (gn.message == null|| !pMsg.matcher(gn.message).find())) return false;
            return true;
        }, timeoutMs, true /* consumeForPopup */);

        // bevorzugt auf Message matchen, sonst Title
        Matcher m = null;
        if (pMsg != null && n.message != null) {
            Matcher mm = pMsg.matcher(n.message);
            if (mm.find()) m = mm;
        }
        if (m == null && pTitle != null && n.title != null) {
            Matcher mt = pTitle.matcher(n.title);
            if (mt.find()) m = mt;
        }
        return new Result(n, m);
    }

    /** Warte auf exakte Severity (INFO/WARN/ERROR/FATAL) – schneller Shortcut. */
    public static Result awaitType(NotificationService svc,
                                   String type, // "INFO" | "WARN" | "ERROR" | "FATAL"
                                   String contextIdOrNull,
                                   String titleRegexOrNull,
                                   String messageRegexOrNull,
                                   long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {

        final String typeNorm = type == null ? null : type.trim().toUpperCase();
        return awaitAny(svc, contextIdOrNull,
                typeNorm == null ? null : Pattern.quote(typeNorm),
                titleRegexOrNull, messageRegexOrNull, timeoutMs);
    }

    // ----------------------------- Result-Typ mit Group-API -----------------------------

    public static final class Result {
        public final GrowlNotification notification;
        private final Matcher matcher; // kann null sein, wenn keine Regex bzw. kein Match

        private Result(GrowlNotification n, Matcher m) {
            this.notification = n;
            this.matcher = m;
        }

        /** group(i) der gematchten Regex (Message bevorzugt, sonst Title). Wirft bei fehlendem Match/Gruppe. */
        public String group(int i) {
            if (matcher == null) throw new IllegalStateException("No regex match available.");
            return matcher.group(i);
        }

        /**
         * group(i) – wenn nicht vorhanden, komplette Message als Fallback (oder null, falls keine Notification).
         * Praktisch für „Nummer aus group(1), sonst volle Meldung“.
         */
        public String groupOrMessage(int i) {
            if (matcher != null) {
                try { return matcher.group(i); } catch (Throwable ignore) {}
            }
            return notification != null ? notification.message : null;
        }

        /** Entweder group(0) (der gesamte Match) oder komplette Message als Fallback. */
        public String matchOrMessage() {
            return groupOrMessage(0);
        }
    }

    // ----------------------------- Helpers -----------------------------

    private static Pattern compileOrNull(String rx) {
        if (rx == null || rx.trim().isEmpty()) return null;
        return Pattern.compile(rx);
    }
}
