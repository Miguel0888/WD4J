package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.service.NotificationService;

import javax.swing.*;
import java.util.Set;
import java.util.WeakHashMap;

public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() {}

    private static final Set<BrowserImpl> HOOKED =
            java.util.Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    /** Einmalig registrieren – zeigt Swing-Dialoge für jede (nicht „verbrauchte“) Growl-Notification an. */
    public static synchronized void hook(BrowserImpl browser) {
        if (browser == null) return;
        if (HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        browser.onNotificationEvent((WDScriptEvent.MessageWD msg) -> {
            try {
                GrowlNotification n = NotificationService.parse(msg);
                if (n == null) return;

                // Service-Instanz je nach Schlüssel (hier: Browser als Key – reicht aus)
                NotificationService svc = NotificationService.getInstance(browser);

                // Nur anzeigen, wenn NICHT durch ein await(...) „verbraucht“
                if (!svc.shouldPopup(n)) return;

                final String caption = "PrimeFaces: " + (n.title == null || n.title.isEmpty() ? n.type : n.title);
                final String body    = (n.message == null ? "" : n.message) + "\n\n(Context: " + n.contextId + ")";
                final int swingType  = mapSeverityToSwing(n.type);

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, body, caption, swingType)
                );
            } catch (Throwable ignore) {
                // fürs erste still — wir wollen das Playback nicht stören
            }
        });
    }

    private static int mapSeverityToSwing(String sev) {
        String s = (sev == null ? "" : sev).toUpperCase();
        if (s.startsWith("WARN"))                 return JOptionPane.WARNING_MESSAGE;
        if (s.startsWith("ERROR") || s.startsWith("FATAL"))
            return JOptionPane.ERROR_MESSAGE;
        return JOptionPane.INFORMATION_MESSAGE;
    }
}
