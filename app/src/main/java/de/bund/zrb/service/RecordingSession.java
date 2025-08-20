package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.RecorderListener;
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

    // Hält die UI-Appender (damit wir beim Stop detach'en können)
    private final List<WDUiAppender> uiAppenders = new ArrayList<WDUiAppender>();

    // Konfiguration für Playwright-Wiring (Attach/Detach-Lambdas)
    private WDEventWiringConfig wiringConfig = WDEventWiringConfig.defaults();

    // Gemeinsame Enable-Flags: gelten sowohl für Page- als auch Context-Mode
    private final EnumMap<WDEventNames, Boolean> eventFlags =
            WDEventFlagPresets.recorderDefaults();

    // Mode
    private boolean contextMode = true;

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

        // UI-Appender mit gemeinsamer Flag-Map + Wiring-Config
        if (contextMode && activeContext != null) {
            for (RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(
                            WDUiAppender.attachToContext(
                                    activeContext,
                                    ((RecorderTabUi) l)::appendEventJson,
                                    wiringConfig,
                                    eventFlags
                            )
                    );
                }
            }
        } else if (!contextMode && activePage != null) {
            for (RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(
                            WDUiAppender.attachToPage(
                                    activePage,
                                    ((RecorderTabUi) l)::appendEventJson,
                                    wiringConfig,
                                    eventFlags
                            )
                    );
                }
            }
        }

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

        // UI-Appender sauber abbauen
        for (WDUiAppender a : uiAppenders) {
            try { a.detachAll(); } catch (Throwable ignore) {}
        }
        uiAppenders.clear();

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
        if (recorderService != null) recorderService.setRecordedActions(actions);
    }

    public synchronized void clearRecordedEvents() {
        if (recorderService != null) recorderService.clearRecordedEvents();
    }

    // ---------- Config (Mode & Event-Flags & Wiring) ----------

    public synchronized void setContextMode(boolean contextMode) { this.contextMode = contextMode; }
    public String getUsername() { return username; }

    /** Ersetzt die gesamte Event-Flag-Map (gilt für Page & Context) und aktualisiert laufende Appender. */
    public synchronized void setEventFlags(Map<WDEventNames, Boolean> newFlags) {
        this.eventFlags.clear();
        if (newFlags != null) this.eventFlags.putAll(newFlags);
        // Live-Update aller laufenden UI-Appender
        for (WDUiAppender a : uiAppenders) {
            a.update(this.eventFlags);
        }
    }

    /** Schaltet ein einzelnes Event an/aus (gilt für Page & Context) und aktualisiert laufende Appender. */
    public synchronized void setEventEnabled(WDEventNames event, boolean enabled) {
        this.eventFlags.put(event, Boolean.valueOf(enabled));
        for (WDUiAppender a : uiAppenders) {
            a.update(this.eventFlags);
        }
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
        for (WDUiAppender a : uiAppenders) {
            try { a.detachAll(); } catch (Throwable ignore) {}
        }
        uiAppenders.clear();

        // neu anhängen mit aktueller wiringConfig + eventFlags
        if (contextMode && activeContext != null) {
            for (RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(
                            WDUiAppender.attachToContext(
                                    activeContext,
                                    ((RecorderTabUi) l)::appendEventJson,
                                    wiringConfig,
                                    eventFlags
                            )
                    );
                }
            }
        } else if (!contextMode && activePage != null) {
            for (RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(
                            WDUiAppender.attachToPage(
                                    activePage,
                                    ((RecorderTabUi) l)::appendEventJson,
                                    wiringConfig,
                                    eventFlags
                            )
                    );
                }
            }
        }
    }
}
