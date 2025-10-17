package de.bund.zrb.util;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class GrowlNotificationPopupUtil {

    private GrowlNotificationPopupUtil() {}

    // pro Browser nur einmal hooken (WeakHashMap verhindert Memory Leaks)
    private static final Set<BrowserImpl> HOOKED =
            java.util.Collections.newSetFromMap(new WeakHashMap<BrowserImpl, Boolean>());

    /** Einmalig registrieren – zeigt Swing-Dialoge für jede PrimeFaces-Growl-Notification an. */
    public static synchronized void hook(BrowserImpl browser) {
        if (browser == null) return;
        if (HOOKED.contains(browser)) return;
        HOOKED.add(browser);

        browser.onNotificationEvent((WDScriptEvent.MessageWD msg) -> {
            try {
                WDRemoteValue payload = msg.getParams().getData();
                if (!(payload instanceof WDRemoteValue.ObjectRemoteValue)) return;

                WDRemoteValue.ObjectRemoteValue root = (WDRemoteValue.ObjectRemoteValue) payload;
                String envelopeType = asString(getProp(root, "type"));
                if (!"growl-event".equals(envelopeType)) return; // nur unsere Growl-Events

                WDRemoteValue dataVal = getProp(root, "data");
                if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return;

                WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) dataVal;

                String severity = asString(getProp(data, "type"));    // INFO|WARN|ERROR|FATAL
                String title    = asString(getProp(data, "title"));
                String message  = asString(getProp(data, "message"));

                String ctxId    = msg.getParams().getSource().getContext().value();

                if (severity == null) severity = "INFO";
                if (title == null)    title = "";
                if (message == null)  message = "";

                final String caption = "PrimeFaces: " + (title.isEmpty() ? severity : title);
                final String body    = message + "\n\n(Context: " + ctxId + ")";
                final int swingType  = mapSeverityToSwing(severity);

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

    // ---- kleine Helfer zum Auslesen der WDRemoteValue-Objekte ----
    private static WDRemoteValue getProp(WDRemoteValue.ObjectRemoteValue obj, String key) {
        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : obj.getValue().entrySet()) {
            if (e.getKey() instanceof WDPrimitiveProtocolValue.StringValue) {
                if (key.equals(((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue()))
                    return e.getValue();
            }
        }
        return null;
    }

    private static String asString(WDRemoteValue v) {
        if (v instanceof WDPrimitiveProtocolValue.StringValue)
            return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.NumberValue)
            return ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue)
            return String.valueOf(((WDPrimitiveProtocolValue.BooleanValue) v).getValue());
        return null;
    }
}
