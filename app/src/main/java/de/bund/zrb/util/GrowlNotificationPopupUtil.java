package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.NotificationService;

import javax.swing.*;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.function.Consumer;

/** Show Swing dialogs only for non-consumed notifications. */
public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() {}

    private static final Set<BrowserImpl> HOOKED =
            Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    /** Register once per browser. */
    public static synchronized void hook(final BrowserImpl browser) {
        if (browser == null) return;
        if (HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        final NotificationService svc = NotificationService.getInstance(browser);
        svc.addSink(new Consumer<GrowlNotification>() {
            @Override
            public void accept(GrowlNotification n) {
                if (!svc.shouldPopup(n)) return;
                final String caption = "PrimeFaces: " + ((n.title == null || n.title.length() == 0) ? n.type : n.title);
                final String body    = (n.message == null ? "" : n.message)
                        + "\n\n(Context: " + (n.contextId == null ? "" : n.contextId) + ")";
                final int swingType  = mapSeverityToSwing(n.type);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, body, caption, swingType);
                    }
                });
            }
        });
    }

    private static int mapSeverityToSwing(String sev) {
        String s = sev == null ? "" : sev.toUpperCase();
        if (s.startsWith("WARN")) return JOptionPane.WARNING_MESSAGE;
        if (s.startsWith("ERROR") || s.startsWith("FATAL")) return JOptionPane.ERROR_MESSAGE;
        return JOptionPane.INFORMATION_MESSAGE;
    }
}
