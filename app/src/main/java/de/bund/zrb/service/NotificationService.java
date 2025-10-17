package de.bund.zrb.service;

import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NotificationService {

    // Instanzen pro Key (Page oder BrowsingContext), analog zu RecorderService
    private static final Map<Object, NotificationService> INSTANCES = new ConcurrentHashMap<>();

    public static synchronized NotificationService getInstance(Object key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null! Use Page or Context.");
        return INSTANCES.computeIfAbsent(key, NotificationService::new);
    }

    public static synchronized void remove(Object key) {
        INSTANCES.remove(key);
    }

    // ----------------------------------------------------------------------------------------------------

    private final Object key;
    private final List<GrowlNotification> buffer = new ArrayList<>();
    private final List<Consumer<List<GrowlNotification>>> listeners = new ArrayList<>();

    private NotificationService(Object key) { this.key = key; }

    public synchronized void addListener(Consumer<List<GrowlNotification>> l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }
    public synchronized void removeListener(Consumer<List<GrowlNotification>> l) {
        listeners.remove(l);
    }
    public synchronized List<GrowlNotification> getAll() {
        return new ArrayList<>(buffer);
    }
    public synchronized void clear() {
        buffer.clear();
        notifyListeners();
    }

    private void notifyListeners() {
        List<GrowlNotification> snapshot = new ArrayList<>(buffer);
        for (Consumer<List<GrowlNotification>> l : listeners) l.accept(snapshot);
    }

    // ----------------------------------------------------------------------------------------------------
    // Entry point aus BrowserImpl.onNotificationEvent(...)
    public void onNotificationMessage(WDScriptEvent.MessageWD msg) {
        GrowlNotification notif = mapGrowlMessage(msg);
        if (notif == null) return;
        synchronized (this) {
            buffer.add(notif);
            notifyListeners();
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Mapping: WDRemoteValue → GrowlNotification (KEIN BrowserImpl-Mapping!)
    private GrowlNotification mapGrowlMessage(WDScriptEvent.MessageWD msg) {
        WDRemoteValue payload = msg.getParams().getData();
        if (!(payload instanceof WDRemoteValue.ObjectRemoteValue)) return null;

        WDRemoteValue.ObjectRemoteValue root = (WDRemoteValue.ObjectRemoteValue) payload;

        // Erwartete Struktur aus primeFacesGrowl.js:
        // { type: "growl-event", data: { type, title, message, timestamp } }
        String envelopeType = asString(getProp(root, "type"));
        if (!"growl-event".equals(envelopeType)) return null;

        WDRemoteValue dataVal = getProp(root, "data");
        if (!(dataVal instanceof WDRemoteValue.ObjectRemoteValue)) return null;
        WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) dataVal;

        String type = asString(getProp(data, "type"));       // INFO|WARN|ERROR|FATAL
        String title = asString(getProp(data, "title"));
        String text  = asString(getProp(data, "message"));
        Long   ts    = asLong  (getProp(data, "timestamp"));

        if (type == null) type = "INFO";
        if (title == null) title = "";
        if (text == null)  text  = "";
        if (ts == null)    ts    = System.currentTimeMillis();

        String contextId = msg.getParams().getSource().getContext().value();
        return new GrowlNotification(contextId, type, title, text, ts);
    }

    // ---- kleine Helfer (kopierbar/gleich wie in deinem Recorder) ----
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
        if (v instanceof WDPrimitiveProtocolValue.StringValue) return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) return ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
        if (v instanceof WDPrimitiveProtocolValue.BooleanValue) return String.valueOf(((WDPrimitiveProtocolValue.BooleanValue) v).getValue());
        return null;
    }
    private static Long asLong(WDRemoteValue v) {
        if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
            try { return Long.parseLong(((WDPrimitiveProtocolValue.NumberValue) v).getValue().split("\\.")[0]); }
            catch (Exception ignore) {}
        }
        return null;
    }
}
