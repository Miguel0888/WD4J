// GrowlNotificationPopupUtil.java (vollständig neu)

package de.bund.zrb.util;

import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.NotificationService;

import javax.swing.*;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Zeigt PrimeFaces-Growl-Meldungen als Swing-Dialoge an – aber ausschließlich über den NotificationService.
 * Meldungen, die durch await(...) konsumiert wurden, werden nicht angezeigt.
 */
public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() {}

    // pro "Hook" (App-Lebenszeit) nur einmal aktivieren
    private static final Set<Object> HOOKED = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Aktiviert die Swing-Popups global über den NotificationService.
     * Es gibt KEIN direktes Abonnement am Browser/onNotificationEvent mehr.
     */
    public static synchronized void hook(Object lifecycleOwner) {
        if (lifecycleOwner == null) lifecycleOwner = GrowlNotificationPopupUtil.class;
        if (HOOKED.contains(lifecycleOwner)) return;
        HOOKED.add(lifecycleOwner);

        // einziges Abo: Service-seitiger globaler Listener
        NotificationService.addGlobalListener(n -> {
            try {
                // Service-Instanz passend zum Context (key = contextId)
                final NotificationService svc = NotificationService.getInstance(n.contextId);

                // Meldungen, die durch await() konsumiert wurden, NICHT anzeigen
                if (!svc.shouldPopup(n)) return;

                final int swingType = mapSeverityToSwing(n.type);
                final String caption = "PrimeFaces: " + (n.title == null || n.title.isEmpty() ? n.type : n.title);
                final String body = (n.message == null ? "" : n.message) + "\n\n(Context: " + n.contextId + ")";

                // leichtes Debounce: UI asynchron
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, body, caption, swingType)
                );
            } catch (Throwable ignore) {
                // Playback nicht stören
            }
        });
    }

    private static int mapSeverityToSwing(String sev) {
        String s = (sev == null ? "" : sev).toUpperCase();
        if (s.startsWith("WARN")) return JOptionPane.WARNING_MESSAGE;
        if (s.startsWith("ERROR") || s.startsWith("FATAL")) return JOptionPane.ERROR_MESSAGE;
        return JOptionPane.INFORMATION_MESSAGE;
    }
}
