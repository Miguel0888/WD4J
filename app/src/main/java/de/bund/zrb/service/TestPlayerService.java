package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.PageImpl;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.*;
import de.bund.zrb.util.GrowlNotificationPopupUtil;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static de.bund.zrb.service.ActivityService.doWithSettling;
import static java.lang.Thread.sleep;

public class TestPlayerService {

    ////////////////////////////////////////////////////////////////////////////////
    // Report-Konfiguration
    ////////////////////////////////////////////////////////////////////////////////

    private static final Path DEFAULT_REPORT_BASE_DIR = Paths.get("C:/Reports"); // aktuell fix, später konfigurierbar
    private static final DateTimeFormatter REPORT_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS"); // Windows-sicher

    private String reportBaseName;     // z.B. 2025-08-12_14-37-22.184
    private Path reportHtmlPath;       // C:/Reports/<base>.html
    private Path reportImagesDir;      // C:/Reports/<base>/
    private int screenshotCounter;     // läuft pro Report hoch

    private static final int QUIET_MS = 500; // 400–600ms hat sich bewährt

    ////////////////////////////////////////////////////////////////////////////////
    // Singleton & Dependencies
    ////////////////////////////////////////////////////////////////////////////////

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final GivenConditionExecutor givenExecutor = new GivenConditionExecutor();

    private TestPlayerUi drawerRef;
    private TestExecutionLogger logger;

    private volatile boolean stopped = false;
    private String lastUsernameUsed = "default";

    private TestPlayerService() {}

    public static TestPlayerService getInstance() {
        return INSTANCE;
    }

    public void registerDrawer(TestPlayerUi playerUi) { this.drawerRef = playerUi; }
    public void registerLogger(TestExecutionLogger logger) { this.logger = logger; }

    public void stopPlayback() { stopped = true; }

    ////////////////////////////////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////////////////////////////////

    public void runSuites() {
        GrowlNotificationPopupUtil.hook(browserService.getBrowser()); // <<< einmalig für notifications registrieren
        ActivityService.getInstance(browserService.getBrowser());
        resetRunFlags();
        if (!isReady()) return;

        beginReport();

        TestNode start = resolveStartNode();
        runNodeStepByStep(start);

        if (stopped) logger.append(new SuiteLog("⏹ Playback abgebrochen!"));

        endReport();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Orchestrierung
    ////////////////////////////////////////////////////////////////////////////////

    private LogComponent runNodeStepByStep(TestNode node) {
        if (stopped || node == null) return null;

        Object model = node.getModelRef();

        if (model instanceof TestAction) {
            return executeActionNode(node, (TestAction) model);
        } else if (model instanceof TestCase) {
            return executeTestCaseNode(node, (TestCase) model);
        } else if (model instanceof TestSuite) {
            return executeSuiteNode(node, (TestSuite) model);
        } else {
            // Fallback: generischer Container-Node (z.B. Ordner)
            return executeGenericContainerNode(node);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Node-Ausführungen
    ////////////////////////////////////////////////////////////////////////////////

    private LogComponent executeActionNode(TestNode node, TestAction action) {
        // Build a log entry for this step. Do not set status here; we set it below.
        StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));

        // Execute the action and capture success/failure. Any exception should
        // mark the step as failed and propagate the error message.
        boolean ok;
        String err = null;
        try {
            ok = playSingleAction(action, stepLog);
            if (!ok) {
                err = "Action returned false";
            }
        } catch (RuntimeException ex) {
            ok = false;
            err = (ex.getMessage() != null) ? ex.getMessage() : ex.toString();
        }

        stepLog.setStatus(ok);
        if (!ok && err != null && !err.isEmpty()) {
            stepLog.setError(err);
        }
        stepLog.setParent(null);             // (optional) keine Hierarchie für Live-Stream nötig
        logger.append(stepLog);              // <— SOFORT anzeigen

        // Update the node status in the tree and propagate to parents
        drawerRef.updateNodeStatus(node, ok);
        // Also update the suite status to reflect any failures in children
        // (This may be redundant if updateNodeStatus already propagates, but it is safe)
        TestNode parent = (TestNode) node.getParent();
        if (parent != null) {
            drawerRef.updateSuiteStatus(parent);
        }
        return stepLog;
    }

    private LogComponent executeTestCaseNode(TestNode node, TestCase testCase) {
        SuiteLog caseLog = new SuiteLog(testCase.getName());
        logger.append(caseLog);                           // <— Überschrift sofort anzeigen

        // GIVEN (Case) – einzeln streamen
        executeGivenList(testCase.getGiven(), caseLog, "Given");

        // WHEN (Kinder) – rekursiv streamen
        executeChildren(node, caseLog);

        // THEN – streamen (macht Screenshot + Bild einbetten)
        executeThenPhase(node, testCase, caseLog);

        drawerRef.updateSuiteStatus(node);
        return caseLog; // Rückgabe optional, wird nicht mehr am Ende appended
    }

    private LogComponent executeSuiteNode(TestNode node, TestSuite suite) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog);                           // <— Header sofort

        executeGivenList(suite.getGiven(), suiteLog, "Suite-Given");
        executeChildren(node, suiteLog);

        drawerRef.updateSuiteStatus(node);
        return suiteLog;
    }

    private LogComponent executeGenericContainerNode(TestNode node) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog);                           // <— Header sofort
        executeChildren(node, suiteLog);
        drawerRef.updateSuiteStatus(node);
        return suiteLog;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Teil-Schritte
    ////////////////////////////////////////////////////////////////////////////////

    private List<LogComponent> executeChildren(TestNode parent, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            LogComponent child = runNodeStepByStep((TestNode) parent.getChildAt(i));
            if (child != null) {
                child.setParent(parentLog);
                out.add(child);
            }
        }
        return out;
    }

    private List<LogComponent> executeGivenList(List<GivenCondition> givens, SuiteLog parentLog, String label) {
        List<LogComponent> out = new ArrayList<>();
        if (givens == null || givens.isEmpty()) return out;

        for (GivenCondition given : givens) {
            StepLog givenLog = new StepLog(label, "Given: " + given.getType());
            try {
                String user = inferUsername(given);
                givenExecutor.apply(user, given);
                givenLog.setStatus(true);
            } catch (Exception ex) {
                givenLog.setStatus(false);
                givenLog.setError(ex.getMessage());
            }
            givenLog.setParent(parentLog);
            logger.append(givenLog);          // <— SOFORT anzeigen
            out.add(givenLog);
        }
        return out;
    }

    private List<LogComponent> executeThenPhase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<>();

        StepLog thenLog = new StepLog("THEN", "Screenshot am Ende des TestCase");
        thenLog.setParent(parentLog);

        try {
            String username = resolveUserForTestCase(caseNode);
            PageImpl page = (PageImpl) browserService.getActivePage(username);

            byte[] png = screenshotAfterWait(3000, page);

            String baseName = (testCase.getName() == null) ? "case" : testCase.getName();
            Path file = saveScreenshotBytes(png, baseName);
            String rel = relToHtml(file);

            thenLog.setStatus(true);
            thenLog.setHtmlAppend(
                    "<img src='" + rel + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>"
            );
        } catch (Exception ex) {
            thenLog.setStatus(false);
            thenLog.setError("Screenshot fehlgeschlagen: " + ex.getMessage());
        }

        logger.append(thenLog);             // <— SOFORT anzeigen
        out.add(thenLog);
        return out;
    }


    /** Erster Action-User im Case-Node, sonst Fallback. */
    private String resolveUserForTestCase(TestNode caseNode) {
        for (int i = 0; i < caseNode.getChildCount(); i++) {
            Object m = ((TestNode) caseNode.getChildAt(i)).getModelRef();
            if (m instanceof TestAction) {
                String u = ((TestAction) m).getUser();
                if (u != null && !u.isEmpty()) return u;
            }
        }
        // Fallback:
        return (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) ? lastUsernameUsed : "default";
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Aktion ausführen (bestehende Logik, nur lesbarer gemacht)
    ////////////////////////////////////////////////////////////////////////////////

    public synchronized boolean playSingleAction(TestAction action, StepLog stepLog) {
        try {
            lastUsernameUsed = action.getUser();
            if (lastUsernameUsed == null || lastUsernameUsed.isEmpty()) {
                System.err.println("⚠️ Keine User-Zuordnung für Action: " + action.getAction());
                return false;
            }

            PageImpl page = (PageImpl) browserService.getActivePage(lastUsernameUsed);
            if (page != null) {
                String contextId = page.getBrowsingContext().value();
                browserService.switchSelectedPage(contextId);
            } else {
                JOptionPane.showMessageDialog(null, "Kein Tab für den im Testfall eingestellten User verfügbar.");
                return false;
            }
            String act = action.getAction();

            switch (act) {
                case "navigate":
                    // Navigation selbst ist die Aktion → wie jede andere: markieren + navigieren + settle
                    return doWithSettling(page, action.getTimeout(), () ->
                            page.navigate(action.getValue(),
                                    new Page.NavigateOptions().setTimeout(action.getTimeout()))
                    );

                case "wait":
                    sleep(Long.parseLong(action.getValue()));
                    return true;

                case "click": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () -> {
                                doWithSettling(page, action.getTimeout(),
                                        () -> loc.click(new Locator.ClickOptions().setTimeout(action.getTimeout())));
                            })
                    );
                    return true;
                }

                case "input":
                case "fill": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () ->
                                    loc.fill(action.getValue(), new Locator.FillOptions().setTimeout(action.getTimeout()))));
                    return true;
                }

                case "select": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () -> loc.selectOption(action.getValue())));
                    return true;
                }

                case "check":
                case "radio": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () ->
                                    loc.check(new Locator.CheckOptions().setTimeout(action.getTimeout()))));
                    return true;
                }

                case "screenshot":
                    byte[] png = screenshotAfterWait(action.getTimeout(), page);

                    String baseName = (stepLog.getName() == null) ? "case" : stepLog.getName();
                    Path file = saveScreenshotBytes(png, baseName);
                    String rel = relToHtml(file);

                    stepLog.setStatus(true);
                    stepLog.setHtmlAppend(
                            "<img src='" + rel + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>"
                    );
                    return true;

                //optional:
                case "press": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () -> loc.press(action.getValue())));
                    return true;
                }
                case "type": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, () ->
                            waitThen(loc, action.getTimeout(), () -> loc.type(action.getValue())));
                    return true;
                }

                default:
                    System.out.println("⚠️ Nicht unterstützte Action: " + act);
                    return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Fehler bei Playback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private byte[] screenshotAfterWait(int timeout, PageImpl page) {
//        sleep(3000);
//        waitForStableBeforeScreenshot(page, timeout);
        // Element- oder Page-Screenshot könnte hier später unterschieden werden.
        return page.screenshot(new Page.ScreenshotOptions().setTimeout(timeout));
    }

    private void waitThen(Locator locator, double timeout, Runnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        action.run();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Logging/Hilfen
    ////////////////////////////////////////////////////////////////////////////////

    private StepLog buildStepLogForAction(TestAction action) {
        // Create a StepLog for the given action. Do not set a status here—
        // the calling code (executeActionNode) will determine pass/fail.
        return new StepLog(action.getType().name(), buildStepText(action));
    }

    private String buildStepText(TestAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(action.getUser()).append(" | ");
        sb.append("Aktion: ").append(action.getAction());
        if (action.getSelectedSelector() != null) {
            sb.append(" @").append(action.getSelectedSelector());
        }
        if (action.getValue() != null) {
            sb.append(" → ").append(action.getValue());
        }
        return sb.toString();
    }

    private String inferUsername(GivenCondition given) {
        Object u = (given.getParameterMap() != null) ? given.getParameterMap().get("username") : null;
        return (u instanceof String && !((String) u).isEmpty()) ? (String) u : "default";
    }

    private void resetRunFlags() { stopped = false; }
    private boolean isReady() { return drawerRef != null && logger != null; }
    private TestNode resolveStartNode() {
        TestNode node = drawerRef.getSelectedNode();
        return (node != null) ? node : drawerRef.getRootNode();
    }

    private Path saveScreenshotBytes(byte[] png, String baseName) throws Exception {
        initReportIfNeeded();
        String safe = (baseName == null || baseName.trim().isEmpty())
                ? "shot"
                : baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = String.format("%03d-%s.png", ++screenshotCounter, safe);
        Path file = reportImagesDir.resolve(fileName);
        Files.write(file, png);
        return file;
    }

    private void withRecordingSuppressed(Page page, Runnable action) {
        try {
            page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }");
        } catch (Throwable ignore) {}
        try {
            action.run();
        } finally {
            try {
                page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }");
            } catch (Throwable ignore) {}
        }
    }

    // Helpers:
    private void beginReport() {
        // 1) Basis-Verzeichnis aus Settings (Fallback: DEFAULT)
        String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class);
        Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty())
                ? java.nio.file.Paths.get(baseDirStr.trim())
                : DEFAULT_REPORT_BASE_DIR;

        reportBaseName    = java.time.LocalDateTime.now().format(REPORT_TS_FMT);
        reportHtmlPath    = baseDir.resolve(reportBaseName + ".html");
        reportImagesDir   = baseDir.resolve(reportBaseName);
        screenshotCounter = 0;

        try {
            java.nio.file.Files.createDirectories(reportImagesDir);
        } catch (Exception ignore) {}

        if (logger != null) {
            // Base setzen, damit relative <img>-Pfade sofort im UI angezeigt werden
            logger.setDocumentBase(baseDir);
        }
    }

    private void initReportIfNeeded() {
        if (reportBaseName != null) return;

        // gleiche Logik wie in beginReport()
        String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class);
        Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty())
                ? java.nio.file.Paths.get(baseDirStr.trim())
                : DEFAULT_REPORT_BASE_DIR;

        reportBaseName    = java.time.LocalDateTime.now().format(REPORT_TS_FMT);
        reportHtmlPath    = baseDir.resolve(reportBaseName + ".html");
        reportImagesDir   = baseDir.resolve(reportBaseName);
        screenshotCounter = 0;

        try {
            java.nio.file.Files.createDirectories(reportImagesDir);
        } catch (Exception e) {
            throw new RuntimeException("Report-Verzeichnisse konnten nicht angelegt werden: " + reportImagesDir, e);
        }

        if (logger != null) {
            logger.setDocumentBase(baseDir);
        }
    }

    private String relToHtml(Path file) {
        Path base = reportHtmlPath.getParent(); // das ist das Report-Basisverzeichnis
        String rel = base.relativize(file).toString();
        return rel.replace('\\', '/');
    }

    private void endReport() {
        if (logger != null) {
            logger.exportAsHtml(reportHtmlPath);
        }
        // Optional: Reset für nächsten Lauf
        // reportBaseName=null; reportHtmlPath=null; reportImagesDir=null; screenshotCounter=0;
    }

    private void waitForStableBeforeScreenshot(Page page, double timeoutMs) {
        page.waitForFunction(
                "quietMs => new Promise((resolve, reject) => {" +
                        "  const start = Date.now();" +
                        "  const tick = () => {" +
                        "    if (Date.now() - start > " + (long) (Integer.MAX_VALUE) + ") { resolve(); }" + // no-op guard
                        "    const getA = window.__zrbGetActivity || (() => null);" +
                        "    const a = getA();" +
                        "    const now = Date.now();" +
                        "    const quiet = a && (a.inflightXHR|0)===0 && (a.inflightFetch|0)===0 && (a.pfQueueDepth|0)===0 && (now - (a.lastChangeTs||0)) >= quietMs;" +
                        "    if (quiet) {" +
                        "      requestAnimationFrame(() => requestAnimationFrame(resolve));" + // 2 Frames fürs Rendern
                        "    } else {" +
                        "      setTimeout(tick, 50);" +
                        "    }" +
                        "  }; tick();" +
                        "})",
                QUIET_MS,
                new Page.WaitForFunctionOptions().setTimeout((int) Math.max(1000, timeoutMs))
        );
    }
}
