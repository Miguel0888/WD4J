package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.RecorderListener;
import de.bund.zrb.ui.debug.WDEventFlagPresets;
import de.bund.zrb.ui.debug.WDEventWiringConfig;
import de.bund.zrb.websocket.WDEventNames;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Own recording lifecycle for exactly one user (context or page mode). */
public final class RecordingSession {

    private final String username;
    private final BrowserService browserService;

    private final List<RecorderListener> listeners = new ArrayList<RecorderListener>();

    private RecorderService recorderService; // underlying recorder (context or page bound)
    private BrowserContext activeContext;
    private Page activePage;
    private boolean recording;

    // NEU: Puffer für Actions, wenn (noch) kein RecorderService existiert
    private List<TestAction> pendingActions;

    /**
     * Records all raw WebDriver events observed during this session. Each entry captures
     * the BiDi event name, the raw payload object and the timestamp when the event
     * was received. These records are used by the EventService to replay or
     * analyse timing for subsequent screenshot and action sequencing.
     */
    private final List<EventRecord> recordedEvents = new ArrayList<EventRecord>();

    // Konfiguration für Playwright-Wiring (Attach/Detach-Lambdas)
    private WDEventWiringConfig wiringConfig = WDEventWiringConfig.defaults();

    // Gemeinsame Enable-Flags: gelten sowohl für Page- als auch Context-Mode
    private final EnumMap<WDEventNames, Boolean> eventFlags =
            WDEventFlagPresets.recorderDefaults();

    // Mode
    private boolean contextMode = true;

    // ---------------------------------------------------------------------------------------------
    // Raw Event Recording
    //
    // The EventService will call recordRawEvent() for each incoming WebDriver event. These
    // records are persisted in-memory for the lifetime of the session and can be retrieved
    // later to determine timing relationships or for export. Synchronised access ensures
    // thread safety when the EventService records events concurrently with UI operations.

    /**
     * Records a raw event with the current timestamp. This method is thread-safe.
     *
     * @param name the BiDi event name (e.g. "network.beforeRequestSent"); must not be null
     * @param raw  the raw payload object associated with the event; may be null
     */
    public synchronized void recordRawEvent(String name, Object raw) {
        if (name == null) return;
        recordedEvents.add(new EventRecord(name, raw, System.currentTimeMillis()));
    }

    /**
     * Returns a copy of all recorded raw events. The returned list is a defensive copy
     * and modifications to it will not affect the internal state of the session. This
     * method is thread-safe.
     *
     * @return a list of recorded events in the order they were received
     */
    public synchronized List<EventRecord> getRecordedRawEvents() {
        return new ArrayList<EventRecord>(recordedEvents);
    }

    /**
     * Determines whether events with the given BiDi name are currently enabled according
     * to the session's event flags. If no entry exists for the corresponding
     * {@link WDEventNames} enum value, the method returns {@code false}.
     *
     * @param name the BiDi event name
     * @return true if the event is enabled, false otherwise
     */
    public synchronized boolean isEventEnabled(String name) {
        if (name == null) return false;
        WDEventNames ev = WDEventNames.fromName(name);
        if (ev == null) return false;
        Boolean flag = eventFlags.get(ev);
        return flag != null && flag.booleanValue();
    }

    /**
     * Exposes the active browser context for this session. May be null if the
     * session has not started or is in page mode. This method is thread-safe.
     *
     * @return the active {@link BrowserContext}, or {@code null} if none
     */
    public synchronized BrowserContext getActiveContext() {
        return activeContext;
    }

    /**
     * Exposes the active page for this session. May be null if the session has not
     * started or is in context mode. This method is thread-safe.
     *
     * @return the active {@link Page}, or {@code null} if none
     */
    public synchronized Page getActivePage() {
        return activePage;
    }

    /**
     * Represents a raw event captured during the recording. Contains the
     * BiDi event name, the raw payload object and the timestamp (in
     * milliseconds since epoch) when the event was recorded. The raw
     * payload is kept as-is and may refer to WebDriver or Playwright
     * internal classes; no defensive copy is created.
     */
    public static final class EventRecord {
        private final String name;
        private final Object raw;
        private final long timestamp;

        public EventRecord(String name, Object raw, long timestamp) {
            this.name = name;
            this.raw = raw;
            this.timestamp = timestamp;
        }

        public String getName() { return name; }
        public Object getRaw() { return raw; }
        public long getTimestamp() { return timestamp; }
    }

    public RecordingSession(String username, BrowserService browserService, boolean contextMode) {
        this.username = Objects.requireNonNull(username, "username");
        this.browserService = Objects.requireNonNull(browserService, "browserService");
        this.contextMode = contextMode;
    }

    // ---------- Lifecycle ----------

    /** Toggle recording; start if currently stopped, else stop. */
    public synchronized void toggle() {
        if (recording) stop(); else start();
    }

    /** Start recording for configured mode. */
    public synchronized void start() {
        if (recording) return;

        UserRegistry.User user = UserRegistry.getInstance().getUser(username);
        if (user == null) throw new IllegalStateException("No user found for: " + username);
        browserService.createUserContext(user);

        if (contextMode) {
            this.activeContext = UserContextMappingService.getInstance().getContextForUser(username);
            this.recorderService = RecorderService.getInstance(activeContext);
            browserService.getBrowser().getRecordingEventRouter().addContextListener(activeContext, recorderService);
        } else {
            this.activePage = browserService.createNewTab(username);
            this.activeContext = activePage.context();
            this.recorderService = RecorderService.getInstance(activePage);
            browserService.getBrowser().getRecordingEventRouter().addPageListener(activePage, recorderService);
        }

        // Wire UI listeners
        for (RecorderListener l : listeners) {
            recorderService.addListener(l);
        }

        // Wenn es gepufferte Actions gibt (z. B. Import vor Start), jetzt anwenden
        if (pendingActions != null) {
            try {
                // RecorderService übernimmt selbst notifyListeners()
                recorderService.setRecordedActions(cloneActions(pendingActions));
            } finally {
                pendingActions = null; // Puffer leeren
            }
        }

        // Skip UI appender wiring for RecorderTabUi. Raw event logging will be
        // handled via EventService instances created by the RecorderTab itself.

        recording = true;
        notifyUiRecordingState(true);
    }

    /** Stop recording and detach listeners. */
    public synchronized void stop() {
        if (!recording) return;

        if (recorderService != null) {
            for (RecorderListener l : listeners) {
                recorderService.removeListener(l);
            }
        }

        if (activePage != null) {
            browserService.getBrowser().getRecordingEventRouter().removePageListener(activePage, recorderService);
            RecorderService.remove(activePage);
        } else if (activeContext != null) {
            browserService.getBrowser().getRecordingEventRouter().removeContextListener(activeContext, recorderService);
            RecorderService.remove(activeContext);
        }

        recorderService = null;
        activePage = null;
        activeContext = null;
        recording = false;
        notifyUiRecordingState(false);
    }

    public synchronized boolean isRecording() { return recording; }

    // ---------- UI Registration ----------

    public synchronized void addListener(RecorderListener l) {
        if (l == null) return;
        listeners.add(l);
        if (recorderService != null) recorderService.addListener(l);
    }

    public synchronized void removeListener(RecorderListener l) {
        if (l == null) return;
        listeners.remove(l);
        if (recorderService != null) recorderService.removeListener(l);
    }

    private void notifyUiRecordingState(boolean on) {
        for (RecorderListener l : listeners) {
            if (l instanceof RecordingStateListener) {
                ((RecordingStateListener) l).onRecordingStateChanged(on);
            }
        }
    }

    // ---------- Drawer operations (delegation) ----------

    public synchronized List<TestAction> getAllTestActionsForDrawer() {
        return (recorderService == null) ? new ArrayList<TestAction>()
                : recorderService.getAllTestActionsForDrawer();
    }

    public synchronized void setRecordedActions(List<TestAction> actions) {
        if (recorderService != null) {
            recorderService.setRecordedActions(actions);
        } else {
            // Noch kein RecorderService → zwischenpuffern (z. B. Import bei gestopptem Recorder)
            this.pendingActions = cloneActions(actions);
        }
    }

    public synchronized void clearRecordedEvents() {
        if (recorderService != null) recorderService.clearRecordedEvents();
        // Falls gestoppt: UI löscht ohnehin direkt; pendingActions nicht antasten
    }

    // ---------- Config (Mode & Event-Flags & Wiring) ----------

    public synchronized void setContextMode(boolean contextMode) { this.contextMode = contextMode; }
    public String getUsername() { return username; }

    /** Ersetzt die gesamte Event-Flag-Map (gilt für Page & Context) und aktualisiert laufende Appender. */
    public synchronized void setEventFlags(Map<WDEventNames, Boolean> newFlags) {
        this.eventFlags.clear();
        if (newFlags != null) this.eventFlags.putAll(newFlags);
        // Live-Update aller laufenden UI-Appender
        // (bereinigt: keine Appender mehr im Einsatz)
    }

    /** Schaltet ein einzelnes Event an/aus (gilt für Page & Context) und aktualisiert laufende Appender. */
    public synchronized void setEventEnabled(WDEventNames event, boolean enabled) {
        this.eventFlags.put(event, Boolean.valueOf(enabled));
        // (bereinigt: keine Appender mehr im Einsatz)
    }

    /** Alias für UI: wird von den Checkboxen aufgerufen. */
    public synchronized void setEventFlag(WDEventNames ev, boolean selected) {
        setEventEnabled(ev, selected);
    }

    /** Defensive Kopie der aktuellen Flags, damit die UI nicht direkt in die Map schreibt. */
    public synchronized EnumMap<WDEventNames, Boolean> getEventFlags() {
        return new EnumMap<WDEventNames, Boolean>(this.eventFlags);
    }

    /** Setzt eine neue Wiring-Config. Wirkt erst nach Stop/Start oder nach {@link #refreshUiWiring()}. */
    public synchronized void setWiringConfig(WDEventWiringConfig cfg) {
        this.wiringConfig = (cfg != null) ? cfg : WDEventWiringConfig.defaults();
    }

    /**
     * Baut die UI-Wiring neu auf (z. B. nach setWiringConfig), falls gerade aufgenommen wird.
     * Stoppt NICHT die eigentliche RecorderService-Pipeline.
     */
    public synchronized void refreshUiWiring() {
        if (!recording) return;

        // alte UI-Appender abbauen
        // (bereinigt: keine Appender mehr im Einsatz)

        // neu anhängen mit aktueller wiringConfig + eventFlags
        // (bereinigt: keine Appender mehr im Einsatz)
    }

    // ---------- intern ----------

    private static List<TestAction> cloneActions(List<TestAction> actions) {
        List<TestAction> out = new ArrayList<TestAction>();
        if (actions == null) return out;
        for (TestAction a : actions) {
            if (a == null) continue;
            out.add(a.copy());
        }
        return out;
    }
}
