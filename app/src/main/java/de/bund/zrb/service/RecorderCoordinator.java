package de.bund.zrb.service;


import java.util.*;

/** Coordinate recording sessions and their tabs across users. */
public final class RecorderCoordinator {

    private static final RecorderCoordinator INSTANCE = new RecorderCoordinator();

    public static RecorderCoordinator getInstance() { return INSTANCE; }

    private final Map<String, RecordingSession> sessions = new HashMap<String, RecordingSession>();
    private final List<RecorderTabUi> tabs = new ArrayList<RecorderTabUi>();

    private RecorderCoordinator() {}

    /** Register a tab UI for a given user and return (or create) its session. */
    public synchronized RecordingSession registerTab(String username, RecorderTabUi tabUi, BrowserService browserService) {
        if (username == null || tabUi == null) throw new IllegalArgumentException("username/tabUi required");
        if (!tabs.contains(tabUi)) tabs.add(tabUi);
        RecordingSession s = sessions.get(username);
        if (s == null) {
            s = new RecordingSession(username, browserService);
            sessions.put(username, s);
        }
        // Ensure UI listens to recorder updates and state changes
        s.addListener(tabUi);
        return s;
    }

    /** Unregister a tab UI (e.g., on tab close). */
    public synchronized void unregisterTab(RecorderTabUi tabUi) {
        if (tabUi == null) return;

        tabs.remove(tabUi);

        RecordingSession s = sessions.get(tabUi.getUsername());
        if (s != null) {
            s.removeListener(tabUi); // detach UI listener
        }

        // If no other tab exists for this user, stop and drop the session
        boolean stillUsed = false;
        for (RecorderTabUi ui : tabs) {
            if (tabUi.getUsername().equals(ui.getUsername())) {
                stillUsed = true;
                break;
            }
        }
        if (!stillUsed) {
            if (s != null && s.isRecording()) {
                s.stop(); // stop recorder on last tab close
            }
            sessions.remove(tabUi.getUsername());
        }
    }

    /** Toggle recording for the currently visible tab. */
    public synchronized void toggleActiveRecording() {
        RecorderTabUi active = findActiveTab();
        if (active == null) return;
        RecordingSession s = sessions.get(active.getUsername());
        if (s != null) s.toggle();
    }

    /** Toggle by username (optional direct addressing). */
    public synchronized void toggleForUser(String username) {
        RecordingSession s = sessions.get(username);
        if (s != null) s.toggle();
    }

    private RecorderTabUi findActiveTab() {
        for (RecorderTabUi ui : tabs) {
            try {
                if (ui.isVisibleActive()) return ui;
            } catch (Throwable ignore) {}
        }
        return null;
    }

    public synchronized void startActiveRecording() {
        RecorderTabUi active = findActiveTab();
        if (active == null) return;
        RecordingSession s = sessions.get(active.getUsername());
        if (s != null && !s.isRecording()) s.start();
    }

    public synchronized void stopActiveRecording() {
        RecorderTabUi active = findActiveTab();
        if (active == null) return;
        RecordingSession s = sessions.get(active.getUsername());
        if (s != null && s.isRecording()) s.stop();
    }

    public synchronized void startForUser(String username) {
        RecordingSession s = sessions.get(username);
        if (s != null && !s.isRecording()) s.start();
    }

    public synchronized void stopForUser(String username) {
        RecordingSession s = sessions.get(username);
        if (s != null && s.isRecording()) s.stop();
    }

}
