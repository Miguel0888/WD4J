package de.bund.zrb.ui.debug;

import de.bund.zrb.service.UserRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

public final class EventMonitorManager {
    private static final Map<UserRegistry.User, EventMonitorWindow> byUser = new IdentityHashMap<>();

    private EventMonitorManager() {}

    public static synchronized EventMonitorWindow getOrCreate(UserRegistry.User user) {
        return byUser.computeIfAbsent(user, u -> new EventMonitorWindow(u.getUsername()));
    }

    public static synchronized void dispose(UserRegistry.User user) {
        EventMonitorWindow w = byUser.remove(user);
        if (w != null) w.dispose();
    }
}
