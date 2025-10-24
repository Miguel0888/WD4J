package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.NotificationService;
import de.bund.zrb.ui.status.StatusBarEventQueue;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Leitet NICHT behandelte (Phase-B) Growl-Notifications in die StatusBarEventQueue.
 * Keine Swing-Dialogs mehr.
 */
public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() {}

    private static final Set<BrowserImpl> HOOKED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    public static synchronized void hook(final BrowserImpl browser) {
        if (browser == null) return;
        if (HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        final NotificationService svc = NotificationService.getInstance(browser);

        // Als "unhandled sink" registrieren: Service ruft uns nur auf, wenn in Phase A niemand konsumiert hat.
        svc.addSink(new Consumer<GrowlNotification>() {
            @Override
            public void accept(final GrowlNotification n) {
                // Baue eine kompakte Statuszeilen-Nachricht
                final String sev = (n.type == null ? "" : n.type).toUpperCase();
                final String prefix =
                        sev.startsWith("ERROR") || sev.startsWith("FATAL") ? "❌ " :
                                sev.startsWith("WARN")                             ? "⚠️ " :
                                        "ℹ️ ";
                final String title = (n.title == null || n.title.isEmpty())
                        ? (n.type == null ? "Hinweis" : n.type)
                        : n.title;
                final String msg = (n.message == null ? "" : n.message);

                String text = prefix + title;
                if (!msg.isEmpty()) text += ": " + msg;

                // Optional Kontext anhängen (kurz)
                if (n.contextId != null && !n.contextId.isEmpty()) {
                    text += "  (" + n.contextId + ")";
                }

                // **WICHTIG**: in die Statusbar-Queue posten (min. Anzeigezeit kommt von der Queue).
                StatusBarEventQueue.getInstance().post(text);
            }
        });
    }
}
