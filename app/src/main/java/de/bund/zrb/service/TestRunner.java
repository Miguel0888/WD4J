package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.PageImpl;
import de.bund.zrb.model.*;
import de.bund.zrb.runtime.*;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.*;
import de.bund.zrb.video.OverlayBridge;
// Netzwerk Debug Imports ---
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.video.overlay.VideoOverlayEvent;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.bund.zrb.service.ActivityService.doWithSettling;

/**
 * TestRunner = eigentlicher Ausführungs-Orchestrator für einen Testrun.
 * <p>
 * Hält:
 * - BrowserService, Logger, UI-Referenzen
 * - TestRunContext (RuntimeVariableContext + Root/Suite/Case-Flags)
 * - Reporting/Screenshot-Handling
 * - Netzwerk-Playback-Logging
 * <p>
 * Der frühere TestPlayerService-Singleton kann später nur noch
 * als dünner Wrapper dienen, der einen neuen TestRunner anlegt
 * und runSuites() aufruft.
 */
public class TestRunner {

    ////////////////////////////////////////////////////////////////////////////////
    // Report-Konfiguration
    /// /////////////////////////////////////////////////////////////////////////////

    private static final Path DEFAULT_REPORT_BASE_DIR = Paths.get("C:/Reports");
    private static final DateTimeFormatter REPORT_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS");

    private String reportBaseName;
    private Path reportHtmlPath;
    private Path reportImagesDir;
    private int screenshotCounter;

    private static final int QUIET_MS = 500; // 400–600ms hat sich bewährt
    private static final String TYPE_PRECONDITION_REF = "preconditionRef";
    private static final long DEFAULT_ASSERT_GROUP_WAIT_MS = 3000L; // fallback if Settings missing
    private static final long DEFAULT_ASSERT_EACH_WAIT_MS = 0L;

    private static final String LOG_LABEL_BEFORE = "ASSIGN";
    private static final String LOG_LABEL_EXECUTION = "ACT";
    private static final String LOG_LABEL_AFTER = "ASSERT";
    private static final String LOG_LABEL_GIVEN = "Given";
    private static final String LOG_LABEL_WHEN = "When";
    private static final String LOG_LABEL_THEN = "Then";

    private static final String AFTER_ASSERTIONS_FAILED_MSG = "After-Assertions fehlgeschlagen";
    private static final String NO_TAB_FOR_USER_MSG = "Kein Tab für den im Testfall eingestellten User verfügbar (%s).";

    private static final String SCREENSHOT_TOOL_BASE = "tool";
    private static final String SCREENSHOT_SHOT_BASE = "shot";
    private static final String SCREENSHOT_CASE_BASE = "case";

    private static final String PRECONDITION_PREFIX = "Precondition: ";
    private static final String GIVEN_PREFIX = "Given: ";

    private static final String CASE_SETUP_FAILED_MSG = "Case-Setup fehlgeschlagen";
    private static final String SUITE_SETUP_FAILED_MSG = "Suite-Setup fehlgeschlagen";
    private static final String PLAYBACK_ABORTED_MSG = "⏹ Playback abgebrochen!";

    private static final String SCREENSHOT_LABEL = "Screenshot";
    private static final String UNSUPPORTED_ACTION_MSG = "⚠️ Nicht unterstützte Action: %s";
    private static final String DEFAULT_USERNAME = "default";

    // --- Playback Logging Zustand ---
    private boolean playbackLoggingActive = false; // was networkLoggingActive
    private final java.util.List<Runnable> playbackUnsubs = new java.util.ArrayList<Runnable>();

    ////////////////////////////////////////////////////////////////////////////////
    // Dependencies
    /// /////////////////////////////////////////////////////////////////////////////

    private final BrowserServiceImpl browserService;
    private final GivenConditionExecutor givenExecutor;
    private final TestPlayerUi drawerRef;
    private final TestExecutionLogger logger;

    ////////////////////////////////////////////////////////////////////////////////
    // Run-spezifischer Kontext
    /// /////////////////////////////////////////////////////////////////////////////

    private TestRunContext runContext; // wird pro runSuites() neu angelegt

    private volatile boolean stopped = false;
    private String lastUsernameUsed = "default";

    ////////////////////////////////////////////////////////////////////////////////
    // Konstruktor

    /// /////////////////////////////////////////////////////////////////////////////

    public TestRunner(BrowserServiceImpl browserService,
                      GivenConditionExecutor givenExecutor,
                      TestPlayerUi drawerRef,
                      TestExecutionLogger logger) {
        this.browserService = browserService;
        this.givenExecutor = givenExecutor;
        this.drawerRef = drawerRef;
        this.logger = logger;
    }

    public void stopPlayback() {
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Public API

    /// /////////////////////////////////////////////////////////////////////////////

    public void runSuites() {
        ActivityService.getInstance(browserService.getBrowser());
        stopped = false;
        if (!isReady()) return;

        // NEU: pro Run frischen Kontext anlegen
        this.runContext = new TestRunContext(ExpressionRegistryImpl.getInstance());
        beginReport();
        setupPlaybackLogging();

        TestNode start = resolveStartNode();
        // Die Initialisierung (Root→Suite→Case) erfolgt vollständig in den execute*-Methoden.
        // Dadurch wird verhindert, dass enterSuite()/enterCase() zuvor gesetzte Werte löscht.

        runNodeStepByStep(start);

        if (stopped) logger.append(new SuiteLog(PLAYBACK_ABORTED_MSG));
        OverlayBridge.clearSubtitle();
        OverlayBridge.clearCaption();
        teardownPlaybackLogging();
        endReport();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Orchestrierung (mit Kontext)

    /// /////////////////////////////////////////////////////////////////////////////

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
            return executeGenericContainerNode(node);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Node-Ausführungen

    /// /////////////////////////////////////////////////////////////////////////////

    private LogComponent executeActionNode(TestNode node, TestAction action) {
        StepLog stepLog = new StepLog(LOG_LABEL_WHEN, buildStepText(action));
        boolean ok;
        String err = null;
        try {
            // Wenn direkt Action gestartet wurde und Case-Init fehlt -> sicherstellen
            TestNode caseNode = (TestNode) node.getParent();
            if (caseNode != null && caseNode.getModelRef() instanceof TestCase) {
                ensureCaseInitForAction(caseNode, (TestCase) caseNode.getModelRef());
            }
            ApplicationEventBus.getInstance().publish(new VideoOverlayEvent(VideoOverlayEvent.Kind.ACTION, action.getType().name()));
            ok = playSingleAction(runContext, action, stepLog);
            if (!ok) err = "Action returned false";
        } catch (RuntimeException ex) {
            ok = false;
            err = (ex.getMessage() != null) ? ex.getMessage() : ex.toString();
        }
        stepLog.setStatus(ok);
        if (!ok && err != null && !err.isEmpty()) stepLog.setError(err);
        stepLog.setParent(null);
        // Speziell: 'screenshot' erzeugt KEINEN When-Log-Eintrag mehr
        if (!"screenshot".equalsIgnoreCase(action.getAction())) {
            logger.append(stepLog);
        }
        drawerRef.updateNodeStatus(node, ok);
        TestNode parent = (TestNode) node.getParent();
        if (parent != null) drawerRef.updateSuiteStatus(parent);
        return stepLog;
    }

    private LogComponent executeTestCaseNode(TestNode node, TestCase testCase) {
        runContext.getVars().enterCase();
        SuiteLog caseLog = new SuiteLog(testCase.getName());
        logger.append(caseLog);
        try {
            ensureUserTabActiveForCase(node);
            // ASSIGN Block für Variablenauswertung + Before-Ketten
            SuiteLog assignBlock = new SuiteLog("ASSIGN");
            assignBlock.setParent(caseLog);
            logger.append(assignBlock);
            initCaseSymbols(node, testCase, assignBlock); // refaktor
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError(CASE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog;
        }
        try {
            runCasePreconditions(node, testCase, caseLog); // Preconditions separat (auch ASSIGN, aber eigener Block)
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError("Precondition execution failed: " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog;
        }
        String sub = (testCase.getName() != null) ? testCase.getName().trim() : "";
        OverlayBridge.setSubtitle(sub);
        ApplicationEventBus.getInstance().publish(new VideoOverlayEvent(VideoOverlayEvent.Kind.CASE, sub));
        // NEU: ACT-Block für die eigentliche Ausführung
        SuiteLog actBlock = new SuiteLog("ACT");
        actBlock.setParent(caseLog);
        logger.append(actBlock);
        executeChildren(node, actBlock);
        executeAfterAssertions(node, testCase, caseLog);
        drawerRef.updateSuiteStatus(node);
        return caseLog;
    }

    private LogComponent executeSuiteNode(TestNode node, TestSuite suite) {
        runContext.getVars().enterSuite();
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog);
        try {
            SuiteLog assignBlock = new SuiteLog("ASSIGN");
            assignBlock.setParent(suiteLog);
            logger.append(assignBlock);
            initSuiteSymbols(suite, assignBlock);
        } catch (Exception ex) {
            String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                    ? suite.getDescription().trim() : (suite.getName() != null ? suite.getName().trim() : node.toString());
            OverlayBridge.setCaption(cap);
            ApplicationEventBus.getInstance().publish(new VideoOverlayEvent(VideoOverlayEvent.Kind.SUITE, cap));
            OverlayBridge.clearSubtitle();
            suiteLog.setStatus(false);
            suiteLog.setError(SUITE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return suiteLog;
        }
        String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                ? suite.getDescription().trim() : (suite.getName() != null ? suite.getName().trim() : node.toString());
        OverlayBridge.setCaption(cap);
        ApplicationEventBus.getInstance().publish(new VideoOverlayEvent(VideoOverlayEvent.Kind.SUITE, cap));
        OverlayBridge.clearSubtitle();
        executeChildren(node, suiteLog);
        drawerRef.updateSuiteStatus(node);
        return suiteLog;
    }

    private LogComponent executeGenericContainerNode(TestNode node) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog);
        executeChildren(node, suiteLog);
        drawerRef.updateSuiteStatus(node);
        return suiteLog;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Teil-Schritte

    /// /////////////////////////////////////////////////////////////////////////////

    private List<LogComponent> executeChildren(TestNode parent, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<LogComponent>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            LogComponent child = runNodeStepByStep((TestNode) parent.getChildAt(i));
            if (child != null) {
                child.setParent(parentLog);
                out.add(child);
            }
        }
        return out;
    }

    private String resolveUserForTestCase(TestNode caseNode) {
        for (int i = 0; i < caseNode.getChildCount(); i++) {
            Object m = ((TestNode) caseNode.getChildAt(i)).getModelRef();
            if (m instanceof TestAction) {
                String u = ((TestAction) m).getUser();
                if (u != null && !u.isEmpty()) return u;
            }
        }
        return (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) ? lastUsernameUsed : DEFAULT_USERNAME;
    }

    // --- Public API for tools: save screenshot into report and return relative path ---
    public String saveScreenshotFromTool(byte[] png, String baseName) throws Exception {
        initReportIfNeeded();
        if (baseName == null || baseName.trim().isEmpty()) baseName = SCREENSHOT_TOOL_BASE;
        Path file = saveScreenshotBytes(png, baseName);
        return relToHtml(file);
    }

    public void logScreenshotFromTool(String label, String relImagePath, boolean ok, String errorMsg) {
        StepLog log = new StepLog(LOG_LABEL_THEN, (label == null || label.trim().isEmpty()) ? SCREENSHOT_LABEL : label);
        log.setStatus(ok);
        if (!ok && errorMsg != null && errorMsg.trim().length() > 0) log.setError(errorMsg.trim());
        if (relImagePath != null && relImagePath.trim().length() > 0) {
            log.setHtmlAppend("<img src='" + relImagePath + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>");
        }
        if (logger != null) logger.append(log);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Aktion ausführen (Kontext erforderlich)

    /// /////////////////////////////////////////////////////////////////////////////

    // Öffentliche Kompatibilitäts-Methode (Single Action außerhalb eines Runs)
    public synchronized boolean playSingleAction(final TestAction action, final StepLog stepLog) {
        TestRunContext isolated = new TestRunContext(ExpressionRegistryImpl.getInstance());
        isolated.getVars().enterCase(); // isolierter Kontext
        return playSingleAction(isolated, action, stepLog);
    }

    private synchronized boolean playSingleAction(final TestRunContext ctx, final TestAction action, final StepLog stepLog) {
        stepLog.setStatus(true); // initial status
        long startNs = System.nanoTime();
        try {
            final String effectiveUser = resolveEffectiveUserForAction(ctx, action);
            lastUsernameUsed = effectiveUser;
            PageImpl page = (PageImpl) browserService.getActivePage(effectiveUser);
            if (page != null) {
                String contextId = page.getBrowsingContext().value();
                browserService.switchSelectedPage(contextId);
            } else {
                JOptionPane.showMessageDialog(null, String.format(NO_TAB_FOR_USER_MSG, effectiveUser));
                stepLog.setStatus(false);
                return false;
            }
            if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                ctx.getVars().setCaseVar("username", effectiveUser.trim());
                ctx.getVars().setCaseVar("user", effectiveUser.trim());
            }
            final ValueScope scopeForThisAction = ctx.getVars().buildCaseScope();
            String act = action.getAction();
            boolean result;
            switch (act) {
                case "navigate": {
                    final String navUrl = resolveActionValueAtRuntime(action, scopeForThisAction);
                    result = doWithSettling(page, action.getTimeout(), new Runnable() {
                        public void run() {
                            page.navigate(navUrl, new Page.NavigateOptions().setTimeout(action.getTimeout()));
                        }
                    });
                    break;
                }
                case "wait":
                    Thread.sleep(Long.parseLong(action.getValue()));
                    result = true; break;
                case "click": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() {
                        public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { doWithSettling(page, action.getTimeout(), new Runnable() { public void run() { loc.click(new Locator.ClickOptions().setTimeout(action.getTimeout())); } }); } }); }
                    });
                    result = true; break;
                }
                case "input":
                case "fill": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() {
                        public void run() throws Exception { waitThen(loc, action.getTimeout(), new ThrowingRunnable() { public void run() throws Exception { final String resolvedText = resolveActionValueAtRuntime(action, scopeForThisAction); loc.fill(resolvedText, new Locator.FillOptions().setTimeout(action.getTimeout())); } }); }
                    });
                    result = true; break;
                }
                case "select": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    final String optionToSelect = resolveActionValueAtRuntime(action, scopeForThisAction);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.selectOption(optionToSelect); } }); } });
                    result = true; break;
                }
                case "check":
                case "radio": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.check(new Locator.CheckOptions().setTimeout(action.getTimeout())); } }); } });
                    result = true; break;
                }
                case "screenshot": {
                    // No-Op: Kein eigenes Bild/Log mehr im Action-Schritt (erst AfterEach-Assertion zeigt das Bild)
                    result = true; break;
                }
                case "press": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.press(action.getValue()); } }); } });
                    result = true; break;
                }
                case "type": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() { public void run() throws Exception { waitThen(loc, action.getTimeout(), new ThrowingRunnable() { public void run() throws Exception { final String resolvedText = resolveActionValueAtRuntime(action, scopeForThisAction); loc.type(resolvedText); } }); } });
                    result = true; break;
                }
                default:
                    System.out.println(String.format(UNSUPPORTED_ACTION_MSG, act));
                    result = false;
            }
            stepLog.setStatus(result);
            return result;
        } catch (ActionEvaluationRuntimeException aerx) {
            Throwable cause = (aerx.getCause() != null) ? aerx.getCause() : aerx;
            stepLog.setStatus(false);
            stepLog.setError(cause.getMessage());
            return false;
        } catch (Exception e) {
            stepLog.setStatus(false);
            stepLog.setError(e.getMessage());
            return false;
        } finally {
            long endNs = System.nanoTime();
            long elapsedMs = (endNs - startNs) / 1_000_000L;
            try {
                // Erst globale Playback-Verzögerung anwenden
                Double delayMs = SettingsService.getInstance().get("playback.delay.currentMs", Double.class);
                if (delayMs != null && delayMs > 0.5) Thread.sleep(Math.round(delayMs));
                // Dann Mindestdauer der Aktion berücksichtigen
                Integer minDur = action.getMinDurationMs();
                if (minDur != null && minDur.intValue() > 0) {
                    long missing = minDur.intValue() - elapsedMs;
                    if (missing > 0) Thread.sleep(missing);
                }
            } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } catch (Throwable ignore) {}
        }
    }


    private void waitThen(Locator locator, double timeout, Runnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        action.run();
    }

    private void waitThen(Locator locator, double timeout, ThrowingRunnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        runUnchecked(action);
    }

    private String buildStepText(TestAction action) {
        if (action == null) return "";
        String desc = action.getDescription();
        if (desc != null && desc.trim().length() > 0) {
            return desc.trim();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(action.getUser()).append(" | ");
        sb.append("Aktion: ").append(action.getAction());
        if (action.getSelectedSelector() != null) sb.append(" @").append(action.getSelectedSelector());
        if (action.getValue() != null) sb.append(" → ").append(action.getValue());
        return sb.toString();
    }

    private String inferUsername(Precondtion given) {
        Object u = (given.getParameterMap() != null) ? given.getParameterMap().get("username") : null;
        if (u instanceof String && !((String) u).isEmpty()) return (String) u;
        if (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) return lastUsernameUsed;
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) return all.get(0).getUsername();
        return "default";
    }

    private boolean isReady() {
        return drawerRef != null && logger != null;
    }

    private TestNode resolveStartNode() {
        TestNode node = drawerRef.getSelectedNode();
        return (node != null) ? node : drawerRef.getRootNode();
    }

    private Path saveScreenshotBytes(byte[] png, String baseName) throws Exception {
        initReportIfNeeded();
        String safe = (baseName == null || baseName.trim().isEmpty()) ? SCREENSHOT_SHOT_BASE : baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = String.format("%03d-%s.png", ++screenshotCounter, safe);
        Path file = reportImagesDir.resolve(fileName);
        Files.write(file, png);
        return file;
    }

    private void withRecordingSuppressed(Page page, Runnable action) {
        try {
            page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }");
        } catch (Throwable ignore) {
        }
        try {
            action.run();
        } finally {
            try {
                page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }");
            } catch (Throwable ignore) {
            }
        }
    }

    private void withRecordingSuppressed(Page page, ThrowingRunnable action) {
        try {
            page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }");
        } catch (Throwable ignore) {
        }
        try {
            runUnchecked(action);
        } finally {
            try {
                page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }");
            } catch (Throwable ignore) {
            }
        }
    }

    private void beginReport() {
        String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class);
        Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty()) ? java.nio.file.Paths.get(baseDirStr.trim()) : DEFAULT_REPORT_BASE_DIR;
        reportBaseName = java.time.LocalDateTime.now().format(REPORT_TS_FMT);
        reportHtmlPath = baseDir.resolve(reportBaseName + ".html");
        reportImagesDir = baseDir.resolve(reportBaseName);
        screenshotCounter = 0;
        try {
            java.nio.file.Files.createDirectories(reportImagesDir);
        } catch (Exception ignore) {
        }
        if (logger != null) logger.setDocumentBase(baseDir);
    }

    private void initReportIfNeeded() {
        if (reportBaseName != null) return;
        String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class);
        Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty()) ? java.nio.file.Paths.get(baseDirStr.trim()) : DEFAULT_REPORT_BASE_DIR;
        reportBaseName = java.time.LocalDateTime.now().format(REPORT_TS_FMT);
        reportHtmlPath = baseDir.resolve(reportBaseName + ".html");
        reportImagesDir = baseDir.resolve(reportBaseName);
        screenshotCounter = 0;
        try {
            java.nio.file.Files.createDirectories(reportImagesDir);
        } catch (Exception e) {
            throw new RuntimeException("Report-Verzeichnisse konnten nicht angelegt werden: " + reportImagesDir, e);
        }
        if (logger != null) logger.setDocumentBase(baseDir);
    }

    private String relToHtml(Path file) {
        Path base = reportHtmlPath.getParent();
        String rel = base.relativize(file).toString();
        return rel.replace('\\', '/');
    }

    // --- Helper für Bildpersistenz/Parsing ---
    private Path saveImageBytes(byte[] bytes, String baseName, String extWithDot) throws Exception {
        initReportIfNeeded();
        String safe = (baseName == null || baseName.trim().isEmpty()) ? SCREENSHOT_SHOT_BASE : baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String ext = (extWithDot != null && extWithDot.startsWith(".")) ? extWithDot : ".png";
        String fileName = String.format("%03d-%s%s", ++screenshotCounter, safe, ext);
        Path file = reportImagesDir.resolve(fileName);
        Files.write(file, bytes);
        return file;
    }

    private String sanitizeBaseName(String s) {
        String t = (s == null ? SCREENSHOT_SHOT_BASE : s.trim());
        if (t.isEmpty()) t = SCREENSHOT_SHOT_BASE;
        return t.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static final class ParsedDataUrl { final String mime; final byte[] bytes; ParsedDataUrl(String m, byte[] b){ this.mime=m; this.bytes=b; } }

    private ParsedDataUrl parseDataUrlSafe(String s) {
        try {
            if (s == null) return null;
            String t = s.trim();
            int comma = t.indexOf(',');
            if (comma < 0) return null;
            String header = t.substring(0, comma).toLowerCase(java.util.Locale.ROOT);
            String payload = t.substring(comma + 1);
            if (!header.startsWith("data:image/") || !header.contains("base64")) return null;
            String mime;
            int semi = header.indexOf(';');
            if (semi > 0) mime = header.substring("data:".length(), semi); else mime = header.substring("data:".length());
            byte[] bytes = java.util.Base64.getDecoder().decode(payload);
            return new ParsedDataUrl(mime, bytes);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private String extFromMime(String mime) {
        if (mime == null) return ".png";
        String m = mime.toLowerCase(java.util.Locale.ROOT);
        if (m.contains("png")) return ".png";
        if (m.contains("jpeg") || m.contains("jpg")) return ".jpg";
        if (m.contains("gif")) return ".gif";
        return ".png";
    }

    private Path resolveExistingFilePath(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            String raw = s.trim();
            Path p;
            if (raw.toLowerCase(java.util.Locale.ROOT).startsWith("file:")) {
                p = java.nio.file.Paths.get(java.net.URI.create(raw));
            } else {
                p = java.nio.file.Paths.get(raw);
                if (!java.nio.file.Files.exists(p) || !java.nio.file.Files.isRegularFile(p)) {
                    if (reportHtmlPath != null) {
                        Path base = reportHtmlPath.getParent();
                        if (base != null) {
                            Path alt = base.resolve(raw).normalize();
                            if (java.nio.file.Files.exists(alt) && java.nio.file.Files.isRegularFile(alt)) {
                                p = alt;
                            }
                        }
                    }
                }
            }
            return (java.nio.file.Files.exists(p) && java.nio.file.Files.isRegularFile(p)) ? p : null;
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * TestRegistry.getInstance().getRoot() kann noch null sein (z.B. während Suite-Setup).
     * Daher hier defensiv prüfen.
     */
    private boolean isRootNodeAvailable() {
        return TestRegistry.getInstance().getRoot() != null;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Report-Handling (Screenshots, Logging)

    /// /////////////////////////////////////////////////////////////////////////////

    private void endReport() {
        if (logger != null) logger.exportAsHtml(reportHtmlPath);
    }

    // Playback Logging
    private void setupPlaybackLogging() {
        if (playbackLoggingActive) return;
        if (!Boolean.getBoolean("wd4j.log.network")) {
            System.out.println("[PLAY] Terminal-Playback-Logging ist deaktiviert (Schalter [Playback]-Logs).");
            return;
        }
        try {
            BrowserImpl browser = browserService.getBrowser();
            if (browser == null) {
                System.err.println("[PLAY] Kein Browser verfügbar – Logging deaktiviert.");
                return;
            }
            WebDriver wd = browser.getWebDriver();
            if (wd == null) {
                System.err.println("[PLAY] Kein WebDriver verfügbar – Logging deaktiviert.");
                return;
            }
            playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.BEFORE_REQUEST_SENT.getName(), new java.util.function.Consumer<Object>() {
                public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.BeforeRequestSent)) return;
                    WDNetworkEvent.BeforeRequestSent e = (WDNetworkEvent.BeforeRequestSent) ev;
                    WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD p = e.getParams();
                    if (p == null || p.getRequest() == null) return;
                    String ctx = p.getContext() != null ? p.getContext().value() : "";
                    String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : "";
                    String method = p.getRequest().getMethod();
                    String url = p.getRequest().getUrl();
                    boolean blocked = p.isBlocked();
                    System.out.printf("[PLAY BEFORE] ctx=%s id=%s %s %s%s%n", ctx, reqId, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : "");
                }
            }));
            playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.RESPONSE_STARTED.getName(), new java.util.function.Consumer<Object>() {
                public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return;
                    WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
                    WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p = e.getParams();
                    if (p == null || p.getRequest() == null) return;
                    String ctx = p.getContext() != null ? p.getContext().value() : "";
                    String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : "";
                    String method = p.getRequest().getMethod();
                    String url = p.getResponse() != null ? p.getResponse().getUrl() : p.getRequest().getUrl();
                    Long status = p.getResponse() != null ? p.getResponse().getStatus() : null;
                    boolean blocked = p.isBlocked();
                    System.out.printf("[PLAY RESP ] ctx=%s id=%s status=%s %s %s%s%n", ctx, reqId, status, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : "");
                }
            }));
            playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.AUTH_REQUIRED.getName(), new java.util.function.Consumer<Object>() {
                public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.AuthRequired)) return;
                    WDNetworkEvent.AuthRequired e = (WDNetworkEvent.AuthRequired) ev;
                    WDNetworkEvent.AuthRequired.AuthRequiredParametersWD p = e.getParams();
                    if (p == null || p.getRequest() == null) return;
                    String ctx = p.getContext() != null ? p.getContext().value() : "";
                    String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : "";
                    String method = p.getRequest().getMethod();
                    String url = p.getRequest().getUrl();
                    boolean blocked = p.isBlocked();
                    System.out.printf("[PLAY AUTH ] ctx=%s id=%s %s %s%s%n", ctx, reqId, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : "");
                }
            }));
            playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.FETCH_ERROR.getName(), new java.util.function.Consumer<Object>() {
                public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.FetchError)) return;
                    WDNetworkEvent.FetchError e = (WDNetworkEvent.FetchError) ev;
                    WDNetworkEvent.FetchError.FetchErrorParametersWD p = e.getParams();
                    if (p == null || p.getRequest() == null) return;
                    String ctx = p.getContext() != null ? p.getContext().value() : "";
                    String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : "";
                    String method = p.getRequest().getMethod();
                    String url = p.getRequest().getUrl();
                    String err = p.getErrorText();
                    System.out.printf("[PLAY FAIL ] ctx=%s id=%s %s %s error=%s%n", ctx, reqId, nullSafe(method), nullSafe(url), nullSafe(err));
                }
            }));
            playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.RESPONSE_COMPLETED.getName(), new java.util.function.Consumer<Object>() {
                public void accept(Object ev) {
                    if (!(ev instanceof WDNetworkEvent.ResponseCompleted)) return;
                    WDNetworkEvent.ResponseCompleted e = (WDNetworkEvent.ResponseCompleted) ev;
                    WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD p = e.getParams();
                    if (p == null || p.getRequest() == null) return;
                    String ctx = p.getContext() != null ? p.getContext().value() : "";
                    String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : "";
                    String method = p.getRequest().getMethod();
                    String url = p.getResponse() != null ? p.getResponse().getUrl() : p.getRequest().getUrl();
                    Long status = p.getResponse() != null ? p.getResponse().getStatus() : null;
                    long bytes = p.getResponse() != null ? p.getResponse().getBytesReceived() : -1L;
                    System.out.printf("[PLAY DONE ] ctx=%s id=%s status=%s bytes=%d %s %s%n", ctx, reqId, status, bytes, nullSafe(method), nullSafe(url));
                }
            }));
            playbackLoggingActive = true;
            System.out.println("[PLAY] Terminal-Playback-Logging aktiviert.");
        } catch (Throwable t) {
            System.err.println("[PLAY] Aktivierung fehlgeschlagen: " + t.getMessage());
        }
    }

    private void teardownPlaybackLogging() {
        if (!playbackLoggingActive) return;
        for (Runnable r : playbackUnsubs) {
            try {
                r.run();
            } catch (Throwable ignore) {
            }
        }
        playbackUnsubs.clear();
        playbackLoggingActive = false;
        System.out.println("[PLAY] Terminal-Playback-Logging deaktiviert.");
    }

    private Runnable subscribeNetwork(WebDriver wd, String eventName, java.util.function.Consumer<Object> handler) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, null, null);
        wd.addEventListener(req, handler);
        return new Runnable() {
            public void run() {
                try {
                    wd.removeEventListener(eventName, (String) null, handler);
                } catch (Throwable ignore) {
                }
            }
        };
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String resolveActionValueAtRuntime(TestAction action, ValueScope scope) throws Exception {
        String template = action.getValue();
        if (template == null) return "";
        return ActionRuntimeEvaluator.evaluateActionValue(template, scope);
    }

    /**
     * User-Auflösung nach gewünschter Regel:
     * 1. Action.user, wenn gesetzt
     * 2. ctx["user"] aus RuntimeVariableContext
     * 3. sonst Fehler
     */
    private String resolveEffectiveUserForAction(TestRunContext ctx, TestAction action) {
        // 1. Use user from action if defined
        if (action != null && action.getUser() != null && !action.getUser().trim().isEmpty()) {
            return action.getUser().trim();
        }
        // 2. Resolve user from RuntimeVariableContext ("user" only)
        if (ctx != null) {
            ValueScope scope = ctx.getVars().buildCaseScope();
            String ctxUser = scope.lookupVar("user");
            if (ctxUser != null && !ctxUser.trim().isEmpty()) {
                return ctxUser.trim();
            }
        }
        // 3. No user found -> fail hard to surface incorrect context initialization
        throw new IllegalStateException("Tool invocation failed: user darf nicht leer sein.");
    }

    private Map<String, String> evaluateExpressionMapNow(Map<String, String> src,
                                                         Map<String, Boolean> enabled,
                                                         RuntimeVariableContext ctx,
                                                         SuiteLog assignParent) throws Exception {
        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<String, String>();
        if (src == null) return out;
        // Versuche passende Description-Map anhand des aktuellen Kontexts zu finden
        Map<String,String> descMap = resolveDescMapForCurrentPhase(src);
        for (Map.Entry<String, String> e : src.entrySet()) {
            String key = e.getKey();
            if (!isEnabled(enabled, key)) continue;
            ValueScope currentScope = ctx.buildCaseScope();
            String existing = currentScope.lookupVar(key);
            if (existing != null && existing.trim().length() > 0) {
                out.put(key, existing);
                continue;
            }
            String exprText = e.getValue();
            String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, currentScope);
            if (resolved == null) resolved = "";
            out.put(key, resolved);
            ctx.setCaseVar(key, resolved);
            // Einzelner ASSIGN-Eintrag als "Given" – mit optionaler Description
            String desc = (descMap != null) ? trimToNull(descMap.get(key)) : null;
            String label = (desc != null) ? desc : (key + " := " + resolved);
            StepLog assign = new StepLog(LOG_LABEL_GIVEN, label);
            assign.setParent(assignParent);
            logger.append(assign);
        }
        return out;
    }

    // Heuristik: ermittelt die passende Description-Map zu einer Variablen-Quelle
    private Map<String,String> resolveDescMapForCurrentPhase(Map<String,String> src) {
        RootNode root = TestRegistry.getInstance().getRoot();
        if (root != null) {
            if (src == root.getBeforeAll()) return root.getBeforeAllDesc();
            if (src == root.getBeforeEach()) return root.getBeforeEachDesc();
        }
        List<TestSuite> suites = TestRegistry.getInstance().getAll();
        if (suites != null) {
            for (int i = 0; i < suites.size(); i++) {
                TestSuite s = suites.get(i);
                if (src == s.getBeforeAll()) return s.getBeforeAllDesc();
                if (src == s.getBeforeEach()) return s.getBeforeEachDesc();
                List<TestCase> cases = s.getTestCases();
                if (cases != null) {
                    for (int c = 0; c < cases.size(); c++) {
                        TestCase tc = cases.get(c);
                        if (src == tc.getBefore()) return tc.getBeforeDesc();
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> filterEnabled(Map<String, String> src, Map<String, Boolean> enabled) {
        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<String, String>();
        if (src == null) return out;
        for (Map.Entry<String, String> e : src.entrySet()) {
            String key = e.getKey();
            if (!isEnabled(enabled, key)) continue;
            out.put(key, e.getValue());
        }
        return out;
    }

    private boolean isEnabled(Map<String, Boolean> enabled, String key) {
        if (enabled == null) return true;
        Boolean val = enabled.get(key);
        return val == null || val.booleanValue();
    }

    private List<LogComponent> executeAfterAssertions(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<LogComponent>();
        try {
            final ValueScope scope = runContext.getVars().buildCaseScope();
            RootNode root = TestRegistry.getInstance().getRoot();
            TestSuite suite = null;
            if (caseNode.getParent() instanceof TestNode) {
                Object parentModel = ((TestNode) caseNode.getParent()).getModelRef();
                if (parentModel instanceof TestSuite) suite = (TestSuite) parentModel;
            }
            java.util.LinkedHashMap<String, String> collected = new java.util.LinkedHashMap<String, String>();
            java.util.LinkedHashMap<String, String> descriptions = new java.util.LinkedHashMap<String, String>();
            java.util.LinkedHashMap<String, String> validatorTypes = new java.util.LinkedHashMap<String, String>();
            java.util.LinkedHashMap<String, String> validatorValues = new java.util.LinkedHashMap<String, String>();
            if (root != null && root.getAfterEach() != null && root.getAfterEachEnabled() != null) {
                Map<String, String> descMap = safeMap(root.getAfterEachDesc());
                Map<String, String> vtMap = safeMap(root.getAfterEachValidatorType());
                Map<String, String> vvMap = safeMap(root.getAfterEachValidatorValue());
                for (java.util.Map.Entry<String, String> e : root.getAfterEach().entrySet()) {
                    String k = e.getKey();
                    Boolean en = root.getAfterEachEnabled().get(k);
                    if (en == null || en.booleanValue()) {
                        String fqk = "Root/" + k;
                        collected.put(fqk, e.getValue());
                        descriptions.put(fqk, trimToNull(descMap.get(k)));
                        validatorTypes.put(fqk, trimToNull(vtMap.get(k)));
                        validatorValues.put(fqk, trimToNull(vvMap.get(k)));
                    }
                }
            }
            if (suite != null && suite.getAfterAll() != null && suite.getAfterAllEnabled() != null) {
                Map<String, String> descMap = safeMap(suite.getAfterAllDesc());
                Map<String, String> vtMap = safeMap(suite.getAfterAllValidatorType());
                Map<String, String> vvMap = safeMap(suite.getAfterAllValidatorValue());
                for (java.util.Map.Entry<String, String> e : suite.getAfterAll().entrySet()) {
                    String k = e.getKey();
                    Boolean en = suite.getAfterAllEnabled().get(k);
                    if (en == null || en.booleanValue()) {
                        String fqk = "Suite/" + k;
                        collected.put(fqk, e.getValue());
                        descriptions.put(fqk, trimToNull(descMap.get(k)));
                        validatorTypes.put(fqk, trimToNull(vtMap.get(k)));
                        validatorValues.put(fqk, trimToNull(vvMap.get(k)));
                    }
                }
            }
            if (testCase.getAfter() != null && testCase.getAfterEnabled() != null) {
                Map<String, String> descMap = safeMap(testCase.getAfterDesc());
                Map<String, String> vtMap = safeMap(testCase.getAfterValidatorType());
                Map<String, String> vvMap = safeMap(testCase.getAfterValidatorValue());
                for (java.util.Map.Entry<String, String> e : testCase.getAfter().entrySet()) {
                    String k = e.getKey();
                    Boolean en = testCase.getAfterEnabled().get(k);
                    if (en == null || en.booleanValue()) {
                        String fqk = "Case/" + k;
                        collected.put(fqk, e.getValue());
                        descriptions.put(fqk, trimToNull(descMap.get(k)));
                        validatorTypes.put(fqk, trimToNull(vtMap.get(k)));
                        validatorValues.put(fqk, trimToNull(vvMap.get(k)));
                    }
                }
            }
            if (collected.isEmpty()) return out;
            SuiteLog afterLog = new SuiteLog(LOG_LABEL_AFTER); // Gruppen-Überschrift bleibt ASSERT
            afterLog.setParent(parentLog);
            logger.append(afterLog);
            out.add(afterLog);
            Integer globalCfg = SettingsService.getInstance().get("assertion.groupWaitMs", Integer.class);
            long globalWaitMs = (globalCfg != null) ? globalCfg.longValue() : DEFAULT_ASSERT_GROUP_WAIT_MS;
            if (globalWaitMs > 0) {
                try {
                    Thread.sleep(globalWaitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            for (java.util.Map.Entry<String, String> e : collected.entrySet()) {
                final String name = e.getKey();
                final String expr = e.getValue();
                final String desc = descriptions.get(name);
                final String displayText = (desc != null && desc.length() > 0) ? (name + " → " + desc) : (name + " → " + shortExpr(expr));
                StepLog assertionLog = new StepLog(LOG_LABEL_THEN, displayText);
                assertionLog.setParent(afterLog);
                try {
                    Integer eachCfg = SettingsService.getInstance().get("assertion.eachWaitMs", Integer.class);
                    long waitMs = (eachCfg != null) ? eachCfg.longValue() : DEFAULT_ASSERT_EACH_WAIT_MS;
                    if (waitMs > 0) {
                        try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                    String result = ActionRuntimeEvaluator.evaluateActionValue(expr, scope);
                    String trimmed = (result == null) ? null : result.trim();
                    String vType = validatorTypes.get(name);
                    String vVal = validatorValues.get(name);
                    boolean ok;
                    String errorMsg = null;
                    if (isBlank(vType)) {
                        ok = true;
                        if (trimmed != null && trimmed.length() > 0) {
                            // Persistiere Bild aus Data-URL oder kopiere existierende Datei in den Report-Bilderordner und binde relativ ein
                            boolean embedded = false;
                            ParsedDataUrl pdu = parseDataUrlSafe(trimmed);
                            if (pdu != null) {
                                String base = sanitizeBaseName(name);
                                String ext = extFromMime(pdu.mime);
                                java.nio.file.Path f = saveImageBytes(pdu.bytes, base, ext);
                                String rel = relToHtml(f);
                                assertionLog.setHtmlAppend("<img src='" + rel + "' alt='Result Image' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem' />");
                                embedded = true;
                            } else {
                                java.nio.file.Path src = resolveExistingFilePath(trimmed);
                                if (src != null) {
                                    byte[] bytes = java.nio.file.Files.readAllBytes(src);
                                    String fn = src.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                                    String ext = fn.endsWith(".png")? ".png" : (fn.endsWith(".jpg")||fn.endsWith(".jpeg"))? ".jpg" : (fn.endsWith(".gif")? ".gif" : ".png");
                                    String base = sanitizeBaseName(name);
                                    java.nio.file.Path f = saveImageBytes(bytes, base, ext);
                                    String rel = relToHtml(f);
                                    assertionLog.setHtmlAppend("<img src='" + rel + "' alt='Result Image' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem' />");
                                    embedded = true;
                                }
                            }
                            if (!embedded) {
                                assertionLog.setHtmlAppend("<small style='color:#666'>Wert: " + trimmed.replace("<", "&lt;").replace(">", "&gt;") + "</small>");
                            }
                        }
                    } else {
                        ok = validateValue(vType, vVal, trimmed);
                        if (!ok) errorMsg = "Validation failed (" + vType + ")" + (trimmed != null ? ": " + trimmed : " (null)");
                    }
                    assertionLog.setStatus(ok);
                    if (!ok && errorMsg != null) assertionLog.setError(errorMsg);
                } catch (Exception ex) {
                    assertionLog.setStatus(false);
                    assertionLog.setError("Exception: " + safeMsg(ex));
                }
                logger.append(assertionLog);
                out.add(assertionLog);
            }
        } catch (Exception ex) {
            StepLog err = new StepLog(LOG_LABEL_THEN, AFTER_ASSERTIONS_FAILED_MSG);
            err.setStatus(false);
            err.setError(safeMsg(ex));
            err.setParent(parentLog);
            logger.append(err);
            out.add(err);
        }
        return out;
    }

    private boolean isDataImageUrl(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(java.util.Locale.ROOT);
        // Akzeptiere data:image/*;base64,... oder data:image/*,...
        if (!t.startsWith("data:image/")) return false;
        // Minimale Plausibilität: enthält ein Komma nach dem Medientyp
        return t.indexOf(',') > 10;
    }

    private String tryBuildDataUrlFromPath(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            String raw = s.trim();
            java.nio.file.Path p;
            if (raw.toLowerCase(java.util.Locale.ROOT).startsWith("file:")) {
                p = java.nio.file.Paths.get(java.net.URI.create(raw));
            } else {
                p = java.nio.file.Paths.get(raw);
                if (!java.nio.file.Files.exists(p) || !java.nio.file.Files.isRegularFile(p)) {
                    // auch relativ zur Report-HTML-Basis versuchen
                    if (reportHtmlPath != null) {
                        java.nio.file.Path base = reportHtmlPath.getParent();
                        if (base != null) {
                            java.nio.file.Path alt = base.resolve(raw).normalize();
                            if (java.nio.file.Files.exists(alt) && java.nio.file.Files.isRegularFile(alt)) {
                                p = alt;
                            }
                        }
                    }
                }
            }
            if (!java.nio.file.Files.exists(p) || !java.nio.file.Files.isRegularFile(p)) return null;
            long size = java.nio.file.Files.size(p);
            if (size <= 0L || size > 20_000_000L) return null; // 20 MB Schutz
            byte[] bytes = java.nio.file.Files.readAllBytes(p);
            String mime = java.nio.file.Files.probeContentType(p);
            if (mime == null) {
                String l = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                if (l.endsWith(".png")) mime = "image/png";
                else if (l.endsWith(".jpg") || l.endsWith(".jpeg")) mime = "image/jpeg";
                else if (l.endsWith(".gif")) mime = "image/gif";
                else return null;
            }
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:" + mime + ";base64," + b64;
        } catch (Throwable ignore) {
            return null;
        }
    }

    private boolean validateValue(String type, String expected, String actual) {
        if (type == null) type = "";
        boolean negate = false;
        if (type.startsWith("!")) {
            negate = true;
            type = type.substring(1);
        }
        String t = type.trim().toLowerCase();
        String exp = expected == null ? "" : expected.trim();
        String act = actual == null ? "" : actual.trim();
        String[] alternatives = exp.contains("||") ? exp.split("\\|\\|") : new String[]{exp};
        boolean any = false;
        for (String altRaw : alternatives) {
            String alt = altRaw.trim();
            if (alt.isEmpty() && t.equals("regex")) {
                any = true;
                break;
            }
            boolean single;
            switch (t) {
                case "regex": {
                    try { single = java.util.regex.Pattern.compile(alt).matcher(act).find(); }
                    catch (Exception ignore) { single = false; }
                    break;
                }
                case "fullregex": {
                    if (alt.isEmpty()) { single = act.isEmpty(); }
                    else {
                        try { single = java.util.regex.Pattern.compile(alt).matcher(act).matches(); }
                        catch (Exception ignore) { single = false; }
                    }
                    break;
                }
                case "contains": single = act.contains(alt); break;
                case "equals":   single = act.equals(alt); break;
                case "starts":   single = act.startsWith(alt); break;
                case "ends":     single = act.endsWith(alt); break;
                case "range": {
                    try {
                        String[] parts = alt.split(":", 2);
                        double min = Double.parseDouble(parts[0]);
                        double max = Double.parseDouble(parts[1]);
                        double val = Double.parseDouble(act);
                        single = val >= min && val <= max;
                    } catch (Exception ignore) { single = false; }
                    break;
                }
                case "len": {
                    try {
                        if (alt.startsWith(">=")) { int n = Integer.parseInt(alt.substring(2)); single = act.length() >= n; }
                        else if (alt.startsWith("<=")) { int n = Integer.parseInt(alt.substring(2)); single = act.length() <= n; }
                        else { int n = Integer.parseInt(alt); single = act.length() == n; }
                    } catch (Exception ignore) { single = false; }
                    break;
                }
                default: single = false;
            }
            if (single) { any = true; break; }
        }
        boolean result = any;
        if (negate) result = !result;
        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String shortExpr(String expr) {
        if (expr == null) return "";
        String t = expr.trim();
        return (t.length() <= 120) ? t : t.substring(0, 117) + "...";
    }

    private static Map<String, String> safeMap(Map<String, String> m) {
        return (m != null) ? m : java.util.Collections.<String, String>emptyMap();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void runUnchecked(ThrowingRunnable op) {
        try {
            op.run();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new ActionEvaluationRuntimeException(ex);
        }
    }

    private String safeMsg(Throwable t) {
        return (t == null) ? "" : (t.getMessage() != null ? t.getMessage() : t.toString());
    }

    private void initSuiteSymbols(final TestSuite suite, SuiteLog assignBlock) throws Exception {
        if (suite == null) return;
        final RootNode rootModel = TestRegistry.getInstance().getRoot();
        final RuntimeVariableContext vars = runContext.getVars();
        java.util.List<ThrowingRunnable> beforeChain = new java.util.ArrayList<ThrowingRunnable>();
        beforeChain.add(new ThrowingRunnable() { public void run() throws Exception {
            if (suite.getId()==null || runContext.isSuiteBeforeAllDone(suite.getId())) return;
            vars.fillSuiteTemplatesFromMap(filterEnabled(suite.getTemplates(), suite.getTemplatesEnabled()));
            Map<String,String> eval = evaluateExpressionMapNow(suite.getBeforeAll(), suite.getBeforeAllEnabled(), vars, assignBlock);
            vars.fillSuiteVarsFromMap(eval);
            runContext.markSuiteBeforeAllDone(suite.getId());
        }});
        beforeChain.add(new ThrowingRunnable() { public void run() throws Exception {
            if (rootModel==null || runContext.isRootBeforeAllDone()) return;
            vars.fillRootTemplatesFromMap(filterEnabled(rootModel.getTemplates(), rootModel.getTemplatesEnabled()));
            Map<String,String> eval = evaluateExpressionMapNow(rootModel.getBeforeAll(), rootModel.getBeforeAllEnabled(), vars, assignBlock);
            vars.fillRootVarsFromMap(eval);
            runContext.markRootBeforeAllDone();
        }});
        beforeChain.add(new ThrowingRunnable() { public void run() throws Exception {
            if (suite.getBeforeEach()==null && suite.getBeforeEachEnabled()==null) return;
            Map<String,String> eval = evaluateExpressionMapNow(suite.getBeforeEach(), suite.getBeforeEachEnabled(), vars, assignBlock);
            vars.fillCaseVarsFromMap(eval);
        }});
        beforeChain.add(new ThrowingRunnable() { public void run() throws Exception {
            if (rootModel==null || (rootModel.getBeforeEach()==null && rootModel.getBeforeEachEnabled()==null)) return;
            Map<String,String> eval = evaluateExpressionMapNow(rootModel.getBeforeEach(), rootModel.getBeforeEachEnabled(), vars, assignBlock);
            vars.fillCaseVarsFromMap(eval);
        }});
        for (ThrowingRunnable r : beforeChain) {
            runUnchecked(r);
            Integer waitCfg = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class);
            long w = (waitCfg!=null)?waitCfg.longValue():0L;
            if (w>0) try { Thread.sleep(w); } catch (InterruptedException ie){ Thread.currentThread().interrupt(); }
        }
    }

    private void initCaseSymbols(final TestNode node, final TestCase testCase, SuiteLog assignBlock) throws Exception {
        if (testCase == null) return;
        final RuntimeVariableContext vars = runContext.getVars();
        final RootNode rootModel = TestRegistry.getInstance().getRoot();
        final TestSuite parentSuite = resolveParentSuite(node);
        vars.fillCaseTemplatesFromMap(filterEnabled(testCase.getTemplates(), testCase.getTemplatesEnabled()));
        if (parentSuite!=null) vars.fillSuiteTemplatesFromMap(filterEnabled(parentSuite.getTemplates(), parentSuite.getTemplatesEnabled()));
        if (rootModel!=null) vars.fillRootTemplatesFromMap(filterEnabled(rootModel.getTemplates(), rootModel.getTemplatesEnabled()));
        runBeforeWithDelay(new ThrowingRunnable(){ public void run() throws Exception {
            Map<String,String> eval = evaluateExpressionMapNow(testCase.getBefore(), testCase.getBeforeEnabled(), vars, assignBlock);
            vars.fillCaseVarsFromMap(eval);
        }});
        if (parentSuite!=null && parentSuite.getId()!=null && !runContext.isSuiteBeforeAllDone(parentSuite.getId())) {
            Map<String,String> eval = evaluateExpressionMapNow(parentSuite.getBeforeAll(), parentSuite.getBeforeAllEnabled(), vars, assignBlock);
            vars.fillSuiteVarsFromMap(eval);
            runContext.markSuiteBeforeAllDone(parentSuite.getId());
        }
        if (!runContext.isRootBeforeAllDone() && rootModel!=null) {
            Map<String,String> eval = evaluateExpressionMapNow(rootModel.getBeforeAll(), rootModel.getBeforeAllEnabled(), vars, assignBlock);
            vars.fillRootVarsFromMap(eval);
            runContext.markRootBeforeAllDone();
        }
        runBeforeWithDelay(new ThrowingRunnable(){ public void run() throws Exception {
            if (parentSuite==null) return;
            Map<String,String> eval = evaluateExpressionMapNow(parentSuite.getBeforeEach(), parentSuite.getBeforeEachEnabled(), vars, assignBlock);
            vars.fillCaseVarsFromMap(eval);
        }});
        runBeforeWithDelay(new ThrowingRunnable(){ public void run() throws Exception {
            if (rootModel==null) return;
            Map<String,String> eval = evaluateExpressionMapNow(rootModel.getBeforeEach(), rootModel.getBeforeEachEnabled(), vars, assignBlock);
            vars.fillCaseVarsFromMap(eval);
        }});
        if (testCase.getId()!=null) runContext.markCaseBeforeChainDone(testCase.getId());
    }

    private void runBeforeWithDelay(ThrowingRunnable step) throws Exception {
        runUnchecked(step);
        Integer waitCfg = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class);
        long w = (waitCfg != null) ? waitCfg.longValue() : 0L;
        if (w > 0L) {
            try {
                Thread.sleep(w);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitAfterBeforeStep(long millis) {
        if (millis <= 0L) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureCaseInitForAction(TestNode caseNode, TestCase testCase) throws RuntimeException {
        try {
            if (testCase.getId() == null || !runContext.isCaseBeforeChainDone(testCase.getId())) {
                runContext.getVars().enterCase();
                // temporärer ASSIGN Block für ad-hoc Initialisierung
                SuiteLog assignBlock = new SuiteLog("ASSIGN");
                logger.append(assignBlock);
                initCaseSymbols(caseNode, testCase, assignBlock);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Initialisierung (ad-hoc) fehlgeschlagen: " + safeMsg(ex), ex);
        }
    }

    private TestSuite resolveParentSuite(TestNode node) {
        if (node == null) return null;
        if (node.getParent() instanceof TestNode) {
            Object pm = ((TestNode) node.getParent()).getModelRef();
            if (pm instanceof TestSuite) return (TestSuite) pm;
        }
        return null;
    }

    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] pairs = value.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) return kv[1];
        }
        return "";
    }

    private String resolvePreconditionName(String id) {
        if (id == null || id.trim().isEmpty()) return "(keine)";
        java.util.List<de.bund.zrb.model.Precondition> list = de.bund.zrb.service.PreconditionRegistry.getInstance().getAll();
        for (de.bund.zrb.model.Precondition p : list) {
            if (id.equals(p.getId())) {
                String n = p.getName();
                return (n != null && n.trim().length() > 0) ? n.trim() : "(unnamed)";
            }
        }
        return id;
    }

    private void waitForStableBeforeScreenshot(Page page, double timeoutMs) {
        long to = (long) Math.max(1000, timeoutMs);
        try {
            page.waitForFunction(
                    "quietMs => {" +
                            "  try {" +
                            "    const getA = (typeof window.__zrbGetActivity === 'function') ? window.__zrbGetActivity : null;" +
                            "    const a    = getA ? getA() : null;" +
                            "    const last = (a && a.lastActivity) ? a.lastActivity : 0;" +
                            "    const since = Date.now() - last;" +
                            "    return since >= quietMs;" +
                            "  } catch (e) { return true; }" +
                            "}",
                    (double) QUIET_MS,
                    new Page.WaitForFunctionOptions().setTimeout(to)
            );
        } catch (Throwable ignore) {
            try {
                page.waitForTimeout(Math.min(to, 1000));
            } catch (Throwable ignore2) {
            }
        }
    }

    private void runCasePreconditions(TestNode caseNode, TestCase testCase, SuiteLog parentLog) throws Exception {
        if (testCase == null) return;
        java.util.List<Precondtion> refs = new java.util.ArrayList<Precondtion>();
        RootNode root = TestRegistry.getInstance().getRoot();
        if (root != null && root.getPreconditions() != null) refs.addAll(root.getPreconditions());
        TestSuite suite = null;
        if (caseNode.getParent() instanceof TestNode) {
            Object parentModel = ((TestNode) caseNode.getParent()).getModelRef();
            if (parentModel instanceof TestSuite) suite = (TestSuite) parentModel;
        }
        if (suite != null && suite.getPreconditions() != null) refs.addAll(suite.getPreconditions());
        if (testCase.getPreconditions() != null) refs.addAll(testCase.getPreconditions());
        if (refs.isEmpty()) return;
        SuiteLog preLog = new SuiteLog("ASSIGN");
        preLog.setParent(parentLog);
        logger.append(preLog);
        for (Precondtion ref : refs) {
            StepLog givenLog = new StepLog(LOG_LABEL_GIVEN, resolvePreconditionName(parseIdFromValue(ref.getValue())));
            try {
                String user = inferUsername(ref);
                givenExecutor.apply(user, ref);
                givenLog.setStatus(true);
            } catch (Exception ex) {
                givenLog.setStatus(false);
                givenLog.setError(safeMsg(ex));
            }
            givenLog.setParent(preLog);
            logger.append(givenLog);
        }
    }

    /**
     * Bestimme den voraussichtlichen User für diesen TestCase und aktiviere dessen Tab,
     * bevor beforeEach-Variablen ausgewertet werden. So laufen alle beforeEach-Schritte
     * im richtigen Vordergrund-Tab und nicht im Hintergrund.
     */
    private void ensureUserTabActiveForCase(TestNode caseNode) {
        try {
            String user = resolveUserForTestCase(caseNode);
            if (user == null || user.trim().isEmpty()) return;
            PageImpl page = (PageImpl) browserService.getActivePage(user);
            if (page == null) return;
            String ctxId = page.getBrowsingContext().value();
            if (ctxId != null && !ctxId.isEmpty()) {
                browserService.switchSelectedPage(ctxId);
                lastUsernameUsed = user; // konsistent halten
            }
        } catch (Throwable ignore) {
            // defensiv still: im Zweifel greift spätere Umschaltung vor der Action wie bisher
        }
    }
}
