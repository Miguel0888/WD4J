package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.service.NotificationService;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Route unhandled (Phase-B) growl notifications into StatusMessageEvent via EventBus.
 */
public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() { }

    private static final Set<BrowserImpl> HOOKED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    public static synchronized void hook(final BrowserImpl browser) {
        if (browser == null) return;
        if (HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        final NotificationService svc = NotificationService.getInstance(browser);

        svc.addSink(new Consumer<GrowlNotification>() {
            @Override
            public void accept(final GrowlNotification n) {
                final String sev = (n.type == null ? "" : n.type).toUpperCase();
                final String prefix = sev.startsWith("ERROR") || sev.startsWith("FATAL") ? "❌ "
                        : sev.startsWith("WARN") ? "⚠️ "
                        : "ℹ️ ";
                final String title = (n.title == null || n.title.isEmpty())
                        ? (n.type == null ? "Hinweis" : n.type)
                        : n.title;
                final String msg = (n.message == null ? "" : n.message);

                String text = prefix + title;
                if (!msg.isEmpty()) text += ": " + msg;
                if (n.contextId != null && !n.contextId.isEmpty()) {
                    text += "  (" + n.contextId + ")";
                }

                // Publish via EventBus (Ticker shows it ~3s)
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent(text, 3000));
            }
        });
    }
}
