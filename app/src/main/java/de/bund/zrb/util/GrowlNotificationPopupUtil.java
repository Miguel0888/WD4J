package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.*;
import de.bund.zrb.service.NotificationService;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() { }

    private static final Set<BrowserImpl> HOOKED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    public static synchronized void hook(final BrowserImpl browser) {
        if (browser == null || HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        final NotificationService svc = NotificationService.getInstance(browser);

        svc.addSink(new Consumer<GrowlNotification>() {
            @Override
            public void accept(final GrowlNotification n) {
                final String sev = (n.type == null ? "" : n.type).toUpperCase();
                final Severity s = sev.startsWith("ERROR") || sev.startsWith("FATAL") ? Severity.ERROR
                        : sev.startsWith("WARN") ? Severity.WARN
                        : Severity.INFO;

                final String title = (n.title == null || n.title.isEmpty())
                        ? (n.type == null ? "Hinweis" : n.type)
                        : n.title;
                final String msg = (n.message == null ? "" : n.message);

                String text = title;
                if (!msg.isEmpty()) text += ": " + msg;
                if (n.contextId != null && !n.contextId.isEmpty()) text += "  (" + n.contextId + ")";

                ApplicationEventBus.getInstance()
                        .publish(new StatusMessageEvent(text, 3000, s));
            }
        });
    }
}
