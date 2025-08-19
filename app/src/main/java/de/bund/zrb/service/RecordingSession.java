package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.RecorderListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Own recording lifecycle for exactly one user (context or page mode). */
public final class RecordingSession {

    private final String username;
    private final BrowserService browserService;

    private final java.util.List<de.bund.zrb.ui.RecorderListener> listeners =
            new java.util.ArrayList<de.bund.zrb.ui.RecorderListener>();

    private RecorderService recorderService; // underlying recorder (context or page bound)
    private BrowserContext activeContext;
    private Page activePage;
    private boolean recording;

    // **NEU: hält die UI-Appender (damit wir beim Stop detach'en können)**
    private final List<WDUiAppender> uiAppenders = new ArrayList<WDUiAppender>();

    // Configuration
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
        for (de.bund.zrb.ui.RecorderListener l : listeners) {
            recorderService.addListener(l);
        }

        if (contextMode && activeContext != null) {
            for (de.bund.zrb.ui.RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(WDUiAppender.attachToContext(activeContext, ((RecorderTabUi) l)::appendEventJson));
                }
            }
        } else if (!contextMode && activePage != null) {
            for (de.bund.zrb.ui.RecorderListener l : listeners) {
                if (l instanceof RecorderTabUi) {
                    uiAppenders.add(WDUiAppender.attachToPage(activePage, ((RecorderTabUi) l)::appendEventJson));
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
            for (de.bund.zrb.ui.RecorderListener l : listeners) {
                recorderService.removeListener(l);
            }
        }

        // **NEU: UI-Appender sauber abbauen**
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
        for (de.bund.zrb.ui.RecorderListener l : listeners) {
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

    // ---------- Config ----------

    public synchronized void setContextMode(boolean contextMode) { this.contextMode = contextMode; }
    public String getUsername() { return username; }
}
