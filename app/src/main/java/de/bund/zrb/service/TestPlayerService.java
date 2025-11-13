package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.StepLog;
import de.bund.zrb.ui.components.log.SuiteLog;
import de.bund.zrb.ui.components.log.TestExecutionLogger;
import de.bund.zrb.video.overlay.VideoOverlayController;

/**
 * TestPlayerService bleibt der zentrale Einstiegspunkt (Singleton)
 * für das UI und andere Teile der Anwendung.
 *
 * Verantwortung:
 *   - Hält Referenzen auf BrowserService, Logger und UI (drawerRef)
 *   - Erzeugt pro Lauf einen neuen TestRunner
 *   - Delegiert Playback-/Screenshot-Aufrufe an den Runner
 *
 * Die eigentliche Orchestrierung des Testruns (Root→Suite→Case→Action)
 * liegt jetzt im TestRunner.
 */
public class TestPlayerService {

    ////////////////////////////////////////////////////////////////////////////////
    // Singleton & Dependencies
    ////////////////////////////////////////////////////////////////////////////////

    private static final TestPlayerService INSTANCE = new TestPlayerService();

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final GivenConditionExecutor givenExecutor = new GivenConditionExecutor();

    private TestPlayerUi drawerRef;
    private TestExecutionLogger logger;

    // --- Laufender Runner (pro Playback) ---
    private TestRunner currentRunner;

    private VideoOverlayController overlayController;

    private TestPlayerService() {
        // enforce singleton
    }

    public static TestPlayerService getInstance() {
        return INSTANCE;
    }

    public void registerDrawer(TestPlayerUi playerUi) {
        this.drawerRef = playerUi;
    }

    public void registerLogger(TestExecutionLogger logger) {
        this.logger = logger;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Stoppt den aktuellen Playback-Lauf (falls vorhanden).
     * Der TestRunner wertet das Flag aus und bricht ab.
     */
    public void stopPlayback() {
        if (currentRunner != null) {
            currentRunner.stopPlayback();
        }
    }

    /**
     * Haupt-Einstiegspunkt für das UI:
     *  - Initialisiert einen neuen TestRunner
     *  - Startet den Lauf (Root/Suite/Case/Action) basierend auf der Auswahl im Tree
     */
    public void runSuites() {
        if (!isReady()) {
            return;
        }
        // Pro Lauf einen frischen Runner mit neuem TestRunContext anlegen
        currentRunner = new TestRunner(
                browserService,
                givenExecutor,
                drawerRef,
                logger
        );

        currentRunner.runSuites();

        if (currentRunner.isStopped()) {
            // Der Runner loggt intern bereits "⏹ Playback abgebrochen!" in den Report,
            // hier kann optional noch zusätzlich reagiert werden.
            logger.append(new SuiteLog("⏹ Playback abgebrochen!"));
        }
    }

    /**
     * Öffentliche Kompatibilitäts-Methode (Single Action außerhalb eines Runs).
     * Delegiert an den TestRunner, der dafür einen isolierten Kontext verwendet.
     */
    public synchronized boolean playSingleAction(final TestAction action, final StepLog stepLog) {
        TestRunner runner = new TestRunner(
                browserService,
                givenExecutor,
                drawerRef,
                logger
        );
        return runner.playSingleAction(action, stepLog);
    }

    /**
     * Public API für Tools: Screenshot speichern und relativen Pfad zurückgeben.
     * Wird an den aktuellen Runner delegiert. Falls kein Runner aktiv ist,
     * wird ad-hoc einer erzeugt, der nur für Reporting/Screenshots zuständig ist.
     */
    public String saveScreenshotFromTool(byte[] png, String baseName) throws Exception {
        ensureRunnerForToolSupport();
        return currentRunner.saveScreenshotFromTool(png, baseName);
    }

    /**
     * Public API für Tools: Screenshot-Logeintrag in den Report schreiben.
     * Delegiert an den aktuellen Runner. Falls kein Runner aktiv ist,
     * wird ad-hoc einer erzeugt, um Logging trotzdem zu ermöglichen.
     */
    public void logScreenshotFromTool(String label, String relImagePath, boolean ok, String errorMsg) {
        ensureRunnerForToolSupport();
        currentRunner.logScreenshotFromTool(label, relImagePath, ok, errorMsg);
    }

    /**
     * Startet den Playback mit Video-Overlay.
     */
    public synchronized void play(TestNode startNode) {
        if (!isReady()) {
            return;
        }
        overlayController = new VideoOverlayController();
        overlayController.start();
        try {
            currentRunner = new TestRunner(
                    browserService,
                    givenExecutor,
                    drawerRef,
                    logger
            );
            currentRunner.runSuites();
        } finally {
            if (overlayController != null) overlayController.stop();
            overlayController = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Hilfsmethoden
    ////////////////////////////////////////////////////////////////////////////////

    private boolean isReady() {
        return drawerRef != null && logger != null;
    }

    /**
     * Stellt sicher, dass für Tool-Aufrufe (Screenshot/Logging) ein Runner existiert.
     * Dieser Runner muss keinen vollständigen Suite-Lauf ausführen, sorgt aber
     * für initialisiertes Reporting/Context.
     */
    private void ensureRunnerForToolSupport() {
        if (currentRunner != null) {
            return;
        }
        // Ad-hoc Runner nur für Reporting/Screenshots/Logging
        currentRunner = new TestRunner(
                browserService,
                givenExecutor,
                drawerRef,
                logger
        );
    }
}
