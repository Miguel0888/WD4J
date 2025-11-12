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
// --- NEU: Netzwerk Debug Imports ---
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.bund.zrb.service.ActivityService.doWithSettling;

public class TestPlayerService {
    ////////////////////////////////////////////////////////////////////////////////
    // Neuer per-Run Kontext Container (zustandsloser Service)
    ////////////////////////////////////////////////////////////////////////////////
    static final class TestRun {
        final RuntimeVariableContext vars;
        boolean rootBeforeAllDone = false;
        final java.util.Set<String> suiteBeforeAllDone = new java.util.HashSet<>();
        final java.util.Set<String> caseBeforeChainDone = new java.util.HashSet<>();
        TestRun() { this.vars = new RuntimeVariableContext(ExpressionRegistryImpl.getInstance()); }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Report-Konfiguration
    ////////////////////////////////////////////////////////////////////////////////

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
    private static final long DEFAULT_ASSERT_EACH_WAIT_MS  = 0L;

    private static final String LOG_LABEL_AFTER  = "AFTER";
    private static final String LOG_LABEL_EXPECT = "EXPECT";
    private static final String LOG_LABEL_ASSERT = "ASSERT";

    private static final String AFTER_ASSERTIONS_FAILED_MSG = "After-Assertions fehlgeschlagen";
    private static final String NO_TAB_FOR_USER_MSG = "Kein Tab für den im Testfall eingestellten User verfügbar (%s).";

    private static final String SCREENSHOT_TOOL_BASE = "tool";
    private static final String SCREENSHOT_SHOT_BASE = "shot";
    private static final String SCREENSHOT_CASE_BASE = "case";

    private static final String PRECONDITION_PREFIX = "Precondition: ";
    private static final String GIVEN_PREFIX = "Given: ";

    private static final String CASE_SETUP_FAILED_MSG  = "Case-Setup fehlgeschlagen";
    private static final String SUITE_SETUP_FAILED_MSG = "Suite-Setup fehlgeschlagen";
    private static final String PLAYBACK_ABORTED_MSG   = "⏹ Playback abgebrochen!";

    private static final String SCREENSHOT_LABEL = "Screenshot";
    private static final String UNSUPPORTED_ACTION_MSG = "⚠️ Nicht unterstützte Action: %s";
    private static final String DEFAULT_USERNAME = "default";

    // --- Playback Logging Zustand ---
    private boolean playbackLoggingActive = false; // was networkLoggingActive
    private final java.util.List<Runnable> playbackUnsubs = new java.util.ArrayList<>();

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
    public static TestPlayerService getInstance() { return INSTANCE; }

    public void registerDrawer(TestPlayerUi playerUi) { this.drawerRef = playerUi; }
    public void registerLogger(TestExecutionLogger logger) { this.logger = logger; }

    public void stopPlayback() { stopped = true; }

    ////////////////////////////////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////////////////////////////////

    public void runSuites() {
        ActivityService.getInstance(browserService.getBrowser());
        stopped = false;
        if (!isReady()) return;

        // NEU: pro Run frischen Kontext anlegen
        TestRun run = new TestRun();
        beginReport();
        setupPlaybackLogging();

        TestNode start = resolveStartNode();
        // Starttypen-spezifische Initialisierung:
        if (start != null) {
            Object model = start.getModelRef();
            try {
                if (model instanceof TestSuite) {
                    // Root/Suite Initialisierung für gewählte Suite
                    initSuiteSymbols(run, (TestSuite) model);
                } else if (model instanceof TestCase) {
                    TestNode caseNode = start;
                    TestCase tc = (TestCase) model;
                    // Suite + Root Init nachholen
                    TestSuite parentSuite = resolveParentSuite(caseNode);
                    initSuiteSymbols(run, parentSuite);
                    run.vars.enterCase();
                    initCaseSymbols(run, caseNode, tc);
                } else if (model instanceof TestAction) {
                    // Action: Suite + Root + Case Init nachholen
                    TestNode caseNode = (TestNode) start.getParent();
                    if (caseNode != null && caseNode.getModelRef() instanceof TestCase) {
                        TestCase tc = (TestCase) caseNode.getModelRef();
                        TestSuite parentSuite = resolveParentSuite(caseNode);
                        initSuiteSymbols(run, parentSuite);
                        run.vars.enterCase();
                        initCaseSymbols(run, caseNode, tc);
                    }
                }
            } catch (Exception ex) {
                SuiteLog err = new SuiteLog("Initialisierung fehlgeschlagen");
                err.setStatus(false); err.setError(safeMsg(ex));
                logger.append(err);
                teardownPlaybackLogging();
                endReport();
                return;
            }
        }

        runNodeStepByStep(run, start);

        if (stopped) logger.append(new SuiteLog(PLAYBACK_ABORTED_MSG));
        OverlayBridge.clearSubtitle();
        OverlayBridge.clearCaption();
        teardownPlaybackLogging();
        endReport();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Orchestrierung (mit Kontext)
    ////////////////////////////////////////////////////////////////////////////////

    private LogComponent runNodeStepByStep(TestRun run, TestNode node) {
        if (stopped || node == null) return null;
        Object model = node.getModelRef();
        if (model instanceof TestAction) {
            return executeActionNode(run, node, (TestAction) model);
        } else if (model instanceof TestCase) {
            return executeTestCaseNode(run, node, (TestCase) model);
        } else if (model instanceof TestSuite) {
            return executeSuiteNode(run, node, (TestSuite) model);
        } else {
            return executeGenericContainerNode(run, node);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Node-Ausführungen
    ////////////////////////////////////////////////////////////////////////////////

    private LogComponent executeActionNode(TestRun run, TestNode node, TestAction action) {
        StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));
        boolean ok; String err = null;
        try {
            // Wenn direkt Action gestartet wurde und Case-Init fehlt -> sicherstellen
            TestNode caseNode = (TestNode) node.getParent();
            if (caseNode != null && caseNode.getModelRef() instanceof TestCase) {
                ensureCaseInitForAction(run, caseNode, (TestCase) caseNode.getModelRef());
            }
            ok = playSingleAction(run, action, stepLog);
            if (!ok) err = "Action returned false";
        } catch (RuntimeException ex) {
            ok = false; err = (ex.getMessage() != null) ? ex.getMessage() : ex.toString();
        }
        stepLog.setStatus(ok);
        if (!ok && err != null && !err.isEmpty()) stepLog.setError(err);
        stepLog.setParent(null);
        logger.append(stepLog);
        drawerRef.updateNodeStatus(node, ok);
        TestNode parent = (TestNode) node.getParent();
        if (parent != null) drawerRef.updateSuiteStatus(parent);
        return stepLog;
    }

    private LogComponent executeTestCaseNode(TestRun run, TestNode node, TestCase testCase) {
        run.vars.enterCase();
        SuiteLog caseLog = new SuiteLog(testCase.getName());
        logger.append(caseLog);
        try {
            TestNode parentNode = (TestNode) node.getParent();
            TestSuite parentSuite = (parentNode != null && parentNode.getModelRef() instanceof TestSuite)
                    ? (TestSuite) parentNode.getModelRef() : null;
            if (parentSuite != null) initSuiteSymbols(run, parentSuite);
            initCaseSymbols(run, node, testCase);
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError(CASE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog;
        }
        try { executePreconditionsForCase(run, node, testCase, caseLog); } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError("Precondition execution failed: " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog;
        }
        String sub = (testCase.getName() != null) ? testCase.getName().trim() : "";
        OverlayBridge.setSubtitle(sub);
        executeChildren(run, node, caseLog);
        executeThenPhase(node, testCase, caseLog); // deprecated
        executeAfterAssertions(run, node, testCase, caseLog);
        drawerRef.updateSuiteStatus(node);
        return caseLog;
    }

    private void executePreconditionsForCase(TestRun run, TestNode caseNode, TestCase testCase, SuiteLog parentLog) throws Exception {
        if (testCase == null) return;
        java.util.List<Precondtion> refs = new java.util.ArrayList<>();
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
        SuiteLog preLog = new SuiteLog(PRECONDITION_PREFIX.trim());
        preLog.setParent(parentLog); logger.append(preLog);
        for (Precondtion ref : refs) {
            if (ref == null) continue;
            if (!TYPE_PRECONDITION_REF.equals(ref.getType())) {
                executeGivenList(run, java.util.Collections.singletonList(ref), preLog, PRECONDITION_PREFIX);
                continue;
            }
            String id = parseIdFromValue(ref.getValue());
            if (id == null || id.trim().isEmpty()) {
                StepLog err = new StepLog(PRECONDITION_PREFIX, "Unbekannte Precondition-Referenz (keine id)");
                err.setStatus(false); err.setParent(preLog); logger.append(err); continue;
            }
            Precondition p = PreconditionRegistry.getInstance().getById(id);
            String displayName = (p != null) ? (p.getName() != null ? p.getName() : id) : id;
            if (p != null && p.getGiven() != null && !p.getGiven().isEmpty()) {
                executeGivenList(run, p.getGiven(), preLog, PRECONDITION_PREFIX + displayName);
            }
            if (p != null && p.getActions() != null && !p.getActions().isEmpty()) {
                for (TestAction pa : p.getActions()) {
                    StepLog stepLog = new StepLog("PRECOND", buildStepText(pa));
                    try {
                        String effectiveUser = (pa.getUser() != null && pa.getUser().trim().length() > 0)
                                ? pa.getUser().trim() : resolveUserForTestCase(caseNode);
                        if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                            run.vars.setCaseVar("username", effectiveUser.trim());
                            run.vars.setCaseVar("user", effectiveUser.trim());
                        }
                        boolean ok = playSingleAction(run, pa, stepLog);
                        stepLog.setStatus(ok);
                        if (!ok) stepLog.setError("Precondition action failed");
                    } catch (Exception ex) {
                        stepLog.setStatus(false); stepLog.setError(safeMsg(ex));
                    }
                    stepLog.setParent(preLog); logger.append(stepLog);
                }
            } else if (p == null) {
                StepLog warn = new StepLog(PRECONDITION_PREFIX, "Precondition not found: " + id);
                warn.setStatus(false); warn.setParent(preLog); logger.append(warn);
            }
        }
    }

    private LogComponent executeSuiteNode(TestRun run, TestNode node, TestSuite suite) {
        run.vars.enterSuite();
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog);
        try { initSuiteSymbols(run, suite); } catch (Exception ex) {
            String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                    ? suite.getDescription().trim() : (suite.getName() != null ? suite.getName().trim() : node.toString());
            OverlayBridge.setCaption(cap); OverlayBridge.clearSubtitle();
            suiteLog.setStatus(false); suiteLog.setError(SUITE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node); return suiteLog;
        }
        String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                ? suite.getDescription().trim() : (suite.getName() != null ? suite.getName().trim() : node.toString());
        OverlayBridge.setCaption(cap); OverlayBridge.clearSubtitle();
        executeChildren(run, node, suiteLog);
        drawerRef.updateSuiteStatus(node); return suiteLog;
    }

    private LogComponent executeGenericContainerNode(TestRun run, TestNode node) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog); executeChildren(run, node, suiteLog); drawerRef.updateSuiteStatus(node); return suiteLog;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Teil-Schritte
    ////////////////////////////////////////////////////////////////////////////////

    private List<LogComponent> executeChildren(TestRun run, TestNode parent, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            LogComponent child = runNodeStepByStep(run, (TestNode) parent.getChildAt(i));
            if (child != null) { child.setParent(parentLog); out.add(child); }
        }
        return out;
    }

    private List<LogComponent> executeGivenList(TestRun run, List<Precondtion> givens, SuiteLog parentLog, String label) {
        List<LogComponent> out = new ArrayList<>(); if (givens == null || givens.isEmpty()) return out;
        for (Precondtion given : givens) {
            String logText;
            if (TYPE_PRECONDITION_REF.equals(given.getType())) {
                String id = parseIdFromValue(given.getValue()); String name = resolvePreconditionName(id); logText = PRECONDITION_PREFIX + name;
            } else { logText = GIVEN_PREFIX + given.getType(); }
            StepLog givenLog = new StepLog(label, logText);
            try {
                String user = inferUsername(given);
                givenExecutor.apply(user, given);
                if (user != null && user.trim().length() > 0) {
                    run.vars.setCaseVar("username", user.trim());
                    run.vars.setCaseVar("user", user.trim());
                }
                if (given.getParameterMap() != null) {
                    for (java.util.Map.Entry<String, Object> entry : given.getParameterMap().entrySet()) {
                        String k = entry.getKey(); Object v = entry.getValue();
                        if (k != null && v instanceof String && ((String) v).trim().length() > 0) {
                            run.vars.setCaseVar(k, ((String) v).trim());
                        }
                    }
                }
                givenLog.setStatus(true);
            } catch (Exception ex) { givenLog.setStatus(false); givenLog.setError(safeMsg(ex)); }
            givenLog.setParent(parentLog); logger.append(givenLog); out.add(givenLog);
        }
        return out;
    }

    @Deprecated // use after
    private List<LogComponent> executeThenPhase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) { return new ArrayList<>(); }

    private String resolveUserForTestCase(TestNode caseNode) {
        for (int i = 0; i < caseNode.getChildCount(); i++) {
            Object m = ((TestNode) caseNode.getChildAt(i)).getModelRef();
            if (m instanceof TestAction) {
                String u = ((TestAction) m).getUser(); if (u != null && !u.isEmpty()) return u;
            }
        }
        return (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) ? lastUsernameUsed : DEFAULT_USERNAME;
    }

    // --- Public API for tools: save screenshot into report and return relative path ---
    public String saveScreenshotFromTool(byte[] png, String baseName) throws Exception {
        initReportIfNeeded(); if (baseName == null || baseName.trim().isEmpty()) baseName = SCREENSHOT_TOOL_BASE;
        Path file = saveScreenshotBytes(png, baseName); return relToHtml(file);
    }

    public void logScreenshotFromTool(String label, String relImagePath, boolean ok, String errorMsg) {
        StepLog log = new StepLog(LOG_LABEL_ASSERT, (label == null || label.trim().isEmpty()) ? SCREENSHOT_LABEL : label);
        log.setStatus(ok);
        if (!ok && errorMsg != null && errorMsg.trim().length() > 0) log.setError(errorMsg.trim());
        if (relImagePath != null && relImagePath.trim().length() > 0) {
            log.setHtmlAppend("<img src='" + relImagePath + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>");
        }
        if (logger != null) logger.append(log);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Aktion ausführen (Kontext erforderlich)
    ////////////////////////////////////////////////////////////////////////////////

    // Öffentliche Kompatibilitäts-Methode (Single Action außerhalb eines Runs)
    public synchronized boolean playSingleAction(final TestAction action, final StepLog stepLog) {
        TestRun run = new TestRun(); run.vars.enterCase(); // isolierter Kontext
        return playSingleAction(run, action, stepLog);
    }

    private synchronized boolean playSingleAction(final TestRun run, final TestAction action, final StepLog stepLog) {
        try {
            final String effectiveUser = resolveEffectiveUserForAction(run, action); lastUsernameUsed = effectiveUser;
            PageImpl page = (PageImpl) browserService.getActivePage(effectiveUser);
            if (page != null) {
                String contextId = page.getBrowsingContext().value(); browserService.switchSelectedPage(contextId);
            } else {
                JOptionPane.showMessageDialog(null, String.format(NO_TAB_FOR_USER_MSG, effectiveUser)); return false;
            }
            if (effectiveUser != null && effectiveUser.trim().length() > 0) { run.vars.setCaseVar("username", effectiveUser.trim()); run.vars.setCaseVar("user", effectiveUser.trim()); }
            final ValueScope scopeForThisAction = run.vars.buildCaseScope();
            String act = action.getAction();
            switch (act) {
                case "navigate": {
                    final String navUrl = resolveActionValueAtRuntime(action, scopeForThisAction);
                    return doWithSettling(page, action.getTimeout(), new Runnable() { public void run() { page.navigate(navUrl, new Page.NavigateOptions().setTimeout(action.getTimeout())); }});
                }
                case "wait": Thread.sleep(Long.parseLong(action.getValue())); return true;
                case "click": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { doWithSettling(page, action.getTimeout(), new Runnable() { public void run() { loc.click(new Locator.ClickOptions().setTimeout(action.getTimeout())); }}); }}); }});
                    return true;
                }
                case "input":
                case "fill": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() { public void run() throws Exception { waitThen(loc, action.getTimeout(), new ThrowingRunnable() { public void run() throws Exception { final String resolvedText = resolveActionValueAtRuntime(action, scopeForThisAction); loc.fill(resolvedText, new Locator.FillOptions().setTimeout(action.getTimeout())); }}); }});
                    return true;
                }
                case "select": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    final String optionToSelect = resolveActionValueAtRuntime(action, scopeForThisAction);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.selectOption(optionToSelect); }}); }});
                    return true;
                }
                case "check":
                case "radio": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.check(new Locator.CheckOptions().setTimeout(action.getTimeout())); }}); }});
                    return true;
                }
                case "screenshot": {
                    byte[] png = screenshotAfterWait(action.getTimeout(), page);
                    String baseName = (stepLog.getName() == null) ? SCREENSHOT_CASE_BASE : stepLog.getName();
                    Path file = saveScreenshotBytes(png, baseName); String rel = relToHtml(file);
                    stepLog.setStatus(true); stepLog.setHtmlAppend("<img src='" + rel + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>");
                    return true;
                }
                case "press": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() { public void run() { waitThen(loc, action.getTimeout(), new Runnable() { public void run() { loc.press(action.getValue()); }}); }});
                    return true;
                }
                case "type": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() { public void run() throws Exception { waitThen(loc, action.getTimeout(), new ThrowingRunnable() { public void run() throws Exception { final String resolvedText = resolveActionValueAtRuntime(action, scopeForThisAction); loc.type(resolvedText); }}); }});
                    return true;
                }
                default: System.out.println(String.format(UNSUPPORTED_ACTION_MSG, act)); return false;
            }
        } catch (ActionEvaluationRuntimeException aerx) {
            Throwable cause = (aerx.getCause() != null) ? aerx.getCause() : aerx; System.err.println("❌ Evaluation failed: " + cause.getMessage()); cause.printStackTrace(); return false;
        } catch (Exception e) {
            System.err.println("❌ Fehler bei Playback: " + e.getMessage()); e.printStackTrace(); return false;
        }
    }

    private byte[] screenshotAfterWait(int timeout, PageImpl page) { waitForStableBeforeScreenshot(page, timeout); return page.screenshot(new Page.ScreenshotOptions().setTimeout(timeout)); }
    private void waitThen(Locator locator, double timeout, Runnable action) { locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout)); action.run(); }
    private void waitThen(Locator locator, double timeout, ThrowingRunnable action) { locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout)); runUnchecked(action); }

    private StepLog buildStepLogForAction(TestAction action) { return new StepLog(action.getType().name(), buildStepText(action)); }
    private String buildStepText(TestAction action) { StringBuilder sb = new StringBuilder(); sb.append("User: ").append(action.getUser()).append(" | "); sb.append("Aktion: ").append(action.getAction()); if (action.getSelectedSelector() != null) sb.append(" @").append(action.getSelectedSelector()); if (action.getValue() != null) sb.append(" → ").append(action.getValue()); return sb.toString(); }
    private String inferUsername(Precondtion given) { Object u = (given.getParameterMap() != null) ? given.getParameterMap().get("username") : null; if (u instanceof String && !((String) u).isEmpty()) return (String) u; if (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) return lastUsernameUsed; java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll(); if (!all.isEmpty()) return all.get(0).getUsername(); return "default"; }

    private boolean isReady() { return drawerRef != null && logger != null; }
    private TestNode resolveStartNode() { TestNode node = drawerRef.getSelectedNode(); return (node != null) ? node : drawerRef.getRootNode(); }

    private Path saveScreenshotBytes(byte[] png, String baseName) throws Exception { initReportIfNeeded(); String safe = (baseName == null || baseName.trim().isEmpty()) ? SCREENSHOT_SHOT_BASE : baseName.replaceAll("[^a-zA-Z0-9._-]", "_"); String fileName = String.format("%03d-%s.png", ++screenshotCounter, safe); Path file = reportImagesDir.resolve(fileName); Files.write(file, png); return file; }
    private void withRecordingSuppressed(Page page, Runnable action) { try { page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }"); } catch (Throwable ignore) {} try { action.run(); } finally { try { page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }"); } catch (Throwable ignore) {} } }
    private void withRecordingSuppressed(Page page, ThrowingRunnable action) { try { page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }"); } catch (Throwable ignore) {} try { runUnchecked(action); } finally { try { page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }"); } catch (Throwable ignore) {} } }

    private void beginReport() { String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class); Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty()) ? java.nio.file.Paths.get(baseDirStr.trim()) : DEFAULT_REPORT_BASE_DIR; reportBaseName = java.time.LocalDateTime.now().format(REPORT_TS_FMT); reportHtmlPath = baseDir.resolve(reportBaseName + ".html"); reportImagesDir = baseDir.resolve(reportBaseName); screenshotCounter = 0; try { java.nio.file.Files.createDirectories(reportImagesDir); } catch (Exception ignore) {} if (logger != null) logger.setDocumentBase(baseDir); }
    private void initReportIfNeeded() { if (reportBaseName != null) return; String baseDirStr = SettingsService.getInstance().get("reportBaseDir", String.class); Path baseDir = (baseDirStr != null && !baseDirStr.trim().isEmpty()) ? java.nio.file.Paths.get(baseDirStr.trim()) : DEFAULT_REPORT_BASE_DIR; reportBaseName = java.time.LocalDateTime.now().format(REPORT_TS_FMT); reportHtmlPath = baseDir.resolve(reportBaseName + ".html"); reportImagesDir = baseDir.resolve(reportBaseName); screenshotCounter = 0; try { java.nio.file.Files.createDirectories(reportImagesDir); } catch (Exception e) { throw new RuntimeException("Report-Verzeichnisse konnten nicht angelegt werden: " + reportImagesDir, e); } if (logger != null) logger.setDocumentBase(baseDir); }
    private String relToHtml(Path file) { Path base = reportHtmlPath.getParent(); String rel = base.relativize(file).toString(); return rel.replace('\\', '/'); }
    private void endReport() { if (logger != null) logger.exportAsHtml(reportHtmlPath); }

    // Playback Logging
    private void setupPlaybackLogging() { if (playbackLoggingActive) return; if (!Boolean.getBoolean("wd4j.log.network")) { System.out.println("[PLAY] Terminal-Playback-Logging ist deaktiviert (Schalter [Playback]-Logs)."); return; } try { BrowserImpl browser = browserService.getBrowser(); if (browser == null) { System.err.println("[PLAY] Kein Browser verfügbar – Logging deaktiviert."); return; } WebDriver wd = browser.getWebDriver(); if (wd == null) { System.err.println("[PLAY] Kein WebDriver verfügbar – Logging deaktiviert."); return; } playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.BEFORE_REQUEST_SENT.getName(), ev -> { if (!(ev instanceof WDNetworkEvent.BeforeRequestSent)) return; WDNetworkEvent.BeforeRequestSent e = (WDNetworkEvent.BeforeRequestSent) ev; WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD p = e.getParams(); if (p == null || p.getRequest() == null) return; String ctx = p.getContext() != null ? p.getContext().value() : ""; String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : ""; String method = p.getRequest().getMethod(); String url = p.getRequest().getUrl(); boolean blocked = p.isBlocked(); System.out.printf("[PLAY BEFORE] ctx=%s id=%s %s %s%s%n", ctx, reqId, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : ""); })); playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.RESPONSE_STARTED.getName(), ev -> { if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return; WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev; WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p = e.getParams(); if (p == null || p.getRequest() == null) return; String ctx = p.getContext() != null ? p.getContext().value() : ""; String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : ""; String method = p.getRequest().getMethod(); String url = p.getResponse() != null ? p.getResponse().getUrl() : p.getRequest().getUrl(); Long status = p.getResponse() != null ? p.getResponse().getStatus() : null; boolean blocked = p.isBlocked(); System.out.printf("[PLAY RESP ] ctx=%s id=%s status=%s %s %s%s%n", ctx, reqId, status, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : ""); })); playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.AUTH_REQUIRED.getName(), ev -> { if (!(ev instanceof WDNetworkEvent.AuthRequired)) return; WDNetworkEvent.AuthRequired e = (WDNetworkEvent.AuthRequired) ev; WDNetworkEvent.AuthRequired.AuthRequiredParametersWD p = e.getParams(); if (p == null || p.getRequest() == null) return; String ctx = p.getContext() != null ? p.getContext().value() : ""; String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : ""; String method = p.getRequest().getMethod(); String url = p.getRequest().getUrl(); boolean blocked = p.isBlocked(); System.out.printf("[PLAY AUTH ] ctx=%s id=%s %s %s%s%n", ctx, reqId, nullSafe(method), nullSafe(url), blocked ? " BLOCKED" : ""); })); playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.FETCH_ERROR.getName(), ev -> { if (!(ev instanceof WDNetworkEvent.FetchError)) return; WDNetworkEvent.FetchError e = (WDNetworkEvent.FetchError) ev; WDNetworkEvent.FetchError.FetchErrorParametersWD p = e.getParams(); if (p == null || p.getRequest() == null) return; String ctx = p.getContext() != null ? p.getContext().value() : ""; String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : ""; String method = p.getRequest().getMethod(); String url = p.getRequest().getUrl(); String err = p.getErrorText(); System.out.printf("[PLAY FAIL ] ctx=%s id=%s %s %s error=%s%n", ctx, reqId, nullSafe(method), nullSafe(url), nullSafe(err)); })); playbackUnsubs.add(subscribeNetwork(wd, WDEventNames.RESPONSE_COMPLETED.getName(), ev -> { if (!(ev instanceof WDNetworkEvent.ResponseCompleted)) return; WDNetworkEvent.ResponseCompleted e = (WDNetworkEvent.ResponseCompleted) ev; WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD p = e.getParams(); if (p == null || p.getRequest() == null) return; String ctx = p.getContext() != null ? p.getContext().value() : ""; String reqId = p.getRequest().getRequest() != null ? p.getRequest().getRequest().value() : ""; String method = p.getRequest().getMethod(); String url = p.getResponse() != null ? p.getResponse().getUrl() : p.getRequest().getUrl(); Long status = p.getResponse() != null ? p.getResponse().getStatus() : null; long bytes = p.getResponse() != null ? p.getResponse().getBytesReceived() : -1L; System.out.printf("[PLAY DONE ] ctx=%s id=%s status=%s bytes=%d %s %s%n", ctx, reqId, status, bytes, nullSafe(method), nullSafe(url)); })); playbackLoggingActive = true; System.out.println("[PLAY] Terminal-Playback-Logging aktiviert."); } catch (Throwable t) { System.err.println("[PLAY] Aktivierung fehlgeschlagen: " + t.getMessage()); } }
    private void teardownPlaybackLogging() { if (!playbackLoggingActive) return; for (Runnable r : playbackUnsubs) { try { r.run(); } catch (Throwable ignore) {} } playbackUnsubs.clear(); playbackLoggingActive = false; System.out.println("[PLAY] Terminal-Playback-Logging deaktiviert."); }
    private Runnable subscribeNetwork(WebDriver wd, String eventName, java.util.function.Consumer<Object> handler) { WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, null, null); wd.addEventListener(req, handler); return () -> { try { wd.removeEventListener(eventName, (String) null, handler); } catch (Throwable ignore) {} }; }
    private String nullSafe(String s) { return s == null ? "" : s; }

    private String resolveActionValueAtRuntime(TestAction action, ValueScope scope) throws Exception { String template = action.getValue(); if (template == null) return ""; return ActionRuntimeEvaluator.evaluateActionValue(template, scope); }
    private String resolveEffectiveUserForAction(TestRun run, TestAction action) {
        // 1. Direkt gesetzter User an der Action?
        if (action.getUser() != null && !action.getUser().trim().isEmpty()) {
            return action.getUser().trim();
        }
        // 2. Aus RuntimeVariableContext (Var "user" oder Fallback "username")
        if (run != null) {
            ValueScope scope = run.vars.buildCaseScope();
            String ctxUser = scope.lookupVar("user");
            if (ctxUser == null || ctxUser.trim().isEmpty()) {
                ctxUser = scope.lookupVar("username");
            }
            if (ctxUser != null && !ctxUser.trim().isEmpty()) {
                return ctxUser.trim();
            }
        }
        // 3. Letzter verwendeter User
        if (lastUsernameUsed != null && !lastUsernameUsed.trim().isEmpty()) {
            return lastUsernameUsed.trim();
        }
        // 4. Registry Fallback
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) {
            UserRegistry.User u = all.get(0);
            if (u != null && u.getUsername() != null && !u.getUsername().trim().isEmpty()) {
                return u.getUsername().trim();
            }
        }
        // 5. Default
        return DEFAULT_USERNAME;
    }

    private Map<String,String> evaluateExpressionMapNow(Map<String,String> src, Map<String, Boolean> enabled, RuntimeVariableContext ctx) throws Exception { java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<>(); if (src == null) return out; for (java.util.Map.Entry<String,String> e : src.entrySet()) { String key = e.getKey(); String exprText = e.getValue(); if (!isEnabled(enabled, key)) continue; ValueScope currentScope = ctx.buildCaseScope(); String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, currentScope); if (resolved == null) resolved = ""; out.put(key, resolved); ctx.setCaseVar(key, resolved); } return out; }
    private Map<String,String> evaluateExpressionMapNowWithScope(Map<String,String> src, Map<String, Boolean> enabled, ValueScope scope, RuntimeVariableContext ctx) throws Exception { java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<>(); if (src == null) return out; for (java.util.Map.Entry<String,String> e : src.entrySet()) { String key = e.getKey(); String exprText = e.getValue(); if (!isEnabled(enabled, key)) continue; String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, scope); if (resolved == null) resolved = ""; out.put(key, resolved); ctx.setCaseVar(key, resolved); } return out; }
    private Map<String,String> filterEnabled(Map<String,String> src, Map<String, Boolean> enabled) { java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<>(); if (src == null) return out; for (java.util.Map.Entry<String,String> e : src.entrySet()) { String key = e.getKey(); if (!isEnabled(enabled, key)) continue; out.put(key, e.getValue()); } return out; }
    private boolean isEnabled(Map<String, Boolean> enabled, String key) { if (enabled == null) return true; Boolean val = enabled.get(key); return val == null || val.booleanValue(); }

    private List<LogComponent> executeAfterAssertions(TestRun run, TestNode caseNode, TestCase testCase, SuiteLog parentLog) { List<LogComponent> out = new ArrayList<>(); try { final ValueScope scope = run.vars.buildCaseScope(); RootNode root = TestRegistry.getInstance().getRoot(); TestSuite suite = null; if (caseNode.getParent() instanceof TestNode) { Object parentModel = ((TestNode) caseNode.getParent()).getModelRef(); if (parentModel instanceof TestSuite) suite = (TestSuite) parentModel; } java.util.LinkedHashMap<String,String> collected = new java.util.LinkedHashMap<>(); java.util.LinkedHashMap<String,String> descriptions = new java.util.LinkedHashMap<>(); java.util.LinkedHashMap<String,String> validatorTypes = new java.util.LinkedHashMap<>(); java.util.LinkedHashMap<String,String> validatorValues = new java.util.LinkedHashMap<>(); if (root != null && root.getAfterEach() != null && root.getAfterEachEnabled() != null) { Map<String,String> descMap = safeMap(root.getAfterEachDesc()); Map<String,String> vtMap = safeMap(root.getAfterEachValidatorType()); Map<String,String> vvMap = safeMap(root.getAfterEachValidatorValue()); for (java.util.Map.Entry<String,String> e : root.getAfterEach().entrySet()) { String k = e.getKey(); Boolean en = root.getAfterEachEnabled().get(k); if (en == null || en.booleanValue()) { String fqk = "Root/" + k; collected.put(fqk, e.getValue()); descriptions.put(fqk, trimToNull(descMap.get(k))); validatorTypes.put(fqk, trimToNull(vtMap.get(k))); validatorValues.put(fqk, trimToNull(vvMap.get(k))); } } }
        if (suite != null && suite.getAfterAll() != null && suite.getAfterAllEnabled() != null) { Map<String,String> descMap = safeMap(suite.getAfterAllDesc()); Map<String,String> vtMap = safeMap(suite.getAfterAllValidatorType()); Map<String,String> vvMap = safeMap(suite.getAfterAllValidatorValue()); for (java.util.Map.Entry<String,String> e : suite.getAfterAll().entrySet()) { String k = e.getKey(); Boolean en = suite.getAfterAllEnabled().get(k); if (en == null || en.booleanValue()) { String fqk = "Suite/" + k; collected.put(fqk, e.getValue()); descriptions.put(fqk, trimToNull(descMap.get(k))); validatorTypes.put(fqk, trimToNull(vtMap.get(k))); validatorValues.put(fqk, trimToNull(vvMap.get(k))); } } }
        if (testCase.getAfter() != null && testCase.getAfterEnabled() != null) { Map<String,String> descMap = safeMap(testCase.getAfterDesc()); Map<String,String> vtMap = safeMap(testCase.getAfterValidatorType()); Map<String,String> vvMap = safeMap(testCase.getAfterValidatorValue()); for (java.util.Map.Entry<String,String> e : testCase.getAfter().entrySet()) { String k = e.getKey(); Boolean en = testCase.getAfterEnabled().get(k); if (en == null || en.booleanValue()) { String fqk = "Case/" + k; collected.put(fqk, e.getValue()); descriptions.put(fqk, trimToNull(descMap.get(k))); validatorTypes.put(fqk, trimToNull(vtMap.get(k))); validatorValues.put(fqk, trimToNull(vvMap.get(k))); } } }
        if (collected.isEmpty()) return out; SuiteLog afterLog = new SuiteLog(LOG_LABEL_AFTER); afterLog.setParent(parentLog); logger.append(afterLog); out.add(afterLog); Integer globalCfg = SettingsService.getInstance().get("assertion.groupWaitMs", Integer.class); long globalWaitMs = (globalCfg != null) ? globalCfg.longValue() : DEFAULT_ASSERT_GROUP_WAIT_MS; if (globalWaitMs > 0) { try { Thread.sleep(globalWaitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } }
        for (java.util.Map.Entry<String,String> e : collected.entrySet()) { final String name = e.getKey(); final String expr = e.getValue(); final String desc = descriptions.get(name); final String displayText = (desc != null && desc.length() > 0) ? (name + " → " + desc) : (name + " → " + shortExpr(expr)); StepLog assertionLog = new StepLog(LOG_LABEL_EXPECT, displayText); assertionLog.setParent(afterLog); try { Integer eachCfg = SettingsService.getInstance().get("assertion.eachWaitMs", Integer.class); long waitMs = (eachCfg != null) ? eachCfg.longValue() : DEFAULT_ASSERT_EACH_WAIT_MS; if (waitMs > 0) { try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } } String result = ActionRuntimeEvaluator.evaluateActionValue(expr, scope); String trimmed = (result == null) ? null : result.trim(); String vType = validatorTypes.get(name); String vVal = validatorValues.get(name); boolean ok; String errorMsg = null; if (isBlank(vType)) { ok = true; if (trimmed != null && trimmed.length() > 0) assertionLog.setHtmlAppend("<small style='color:#666'>Wert: " + trimmed.replace("<","&lt;").replace(">","&gt;") + "</small>"); } else { ok = validateValue(vType, vVal, trimmed); if (!ok) errorMsg = "Validation failed (" + vType + ")" + (trimmed != null ? ": " + trimmed : " (null)"); } assertionLog.setStatus(ok); if (!ok && errorMsg != null) assertionLog.setError(errorMsg); } catch (Exception ex) { assertionLog.setStatus(false); assertionLog.setError("Exception: " + safeMsg(ex)); } logger.append(assertionLog); out.add(assertionLog); }
    } catch (Exception ex) { StepLog err = new StepLog(LOG_LABEL_AFTER, AFTER_ASSERTIONS_FAILED_MSG); err.setStatus(false); err.setError(safeMsg(ex)); err.setParent(parentLog); logger.append(err); out.add(err); }
        return out; }

    private boolean validateValue(String type, String expected, String actual) { if (type == null) type = ""; boolean negate = false; if (type.startsWith("!")) { negate = true; type = type.substring(1); } String t = type.trim().toLowerCase(); String exp = expected == null ? "" : expected.trim(); String act = actual == null ? "" : actual.trim(); String[] alternatives = exp.contains("||") ? exp.split("\\|\\|") : new String[]{exp}; boolean any = false; for (String altRaw : alternatives) { String alt = altRaw.trim(); if (alt.isEmpty() && t.equals("regex")) { any = true; break; } boolean single; switch (t) { case "regex": { try { single = java.util.regex.Pattern.compile(alt).matcher(act).find(); } catch (Exception ignore) { single = false; } break; } case "fullregex": { if (alt.isEmpty()) { single = act.isEmpty(); } else { try { single = java.util.regex.Pattern.compile(alt).matcher(act).matches(); } catch (Exception ignore) { single = false; } } break; } case "contains": single = act.contains(alt); break; case "equals": single = act.equals(alt); break; case "starts": single = act.startsWith(alt); break; case "ends": single = act.endsWith(alt); break; case "range": { try { String[] parts = alt.split(":", 2); double min = Double.parseDouble(parts[0]); double max = Double.parseDouble(parts[1]); double val = Double.parseDouble(act); single = val >= min && val <= max; } catch (Exception ignore) { single = false; } break; } case "len": { try { if (alt.startsWith(">=")) { int n = Integer.parseInt(alt.substring(2)); single = act.length() >= n; } else if (alt.startsWith("<=")) { int n = Integer.parseInt(alt.substring(2)); single = act.length() <= n; } else { int n = Integer.parseInt(alt); single = act.length() == n; } } catch (Exception ignore) { single = false; } break; } default: single = false; } if (single) { any = true; break; } } boolean result = any; if (negate) result = !result; return result; }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String trimToNull(String s) { if (s == null) return null; String t = s.trim(); return t.isEmpty() ? null : t; }
    private String shortExpr(String expr) { if (expr == null) return ""; String t = expr.trim(); return (t.length() <= 120) ? t : t.substring(0,117)+"..."; }
    private static Map<String,String> safeMap(Map<String,String> m) { return (m != null) ? m : java.util.Collections.<String,String>emptyMap(); }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
    private void runUnchecked(ThrowingRunnable op) { try { op.run(); } catch (RuntimeException re) { throw re; } catch (Exception ex) { throw new ActionEvaluationRuntimeException(ex); } }
    private String safeMsg(Throwable t) { return (t == null) ? "" : (t.getMessage() != null ? t.getMessage() : t.toString()); }

    // Suite-/Case-Initialisierung jetzt mit TestRun
    private void initSuiteSymbols(TestRun run, TestSuite suite) throws Exception {
        if (suite == null) return; RootNode rootModel = TestRegistry.getInstance().getRoot();
        // Root: Templates zuerst, dann BeforeAll (nur einmal pro Run)
        if (!run.rootBeforeAllDone && rootModel != null) {
            // Templates (lazy expressions) immer vor den BeforeAll-Werten
            run.vars.fillRootTemplatesFromMap(filterEnabled(rootModel.getTemplates(), rootModel.getTemplatesEnabled()));
            Map<String,String> evaluated = evaluateExpressionMapNow(rootModel.getBeforeAll(), rootModel.getBeforeAllEnabled(), run.vars);
            run.vars.fillRootVarsFromMap(evaluated);
            run.rootBeforeAllDone = true;
        }
        // Suite: Templates zuerst, dann BeforeAll (nur einmal pro Suite im Run)
        if (suite.getId() != null && !run.suiteBeforeAllDone.contains(suite.getId())) {
            run.vars.fillSuiteTemplatesFromMap(filterEnabled(suite.getTemplates(), suite.getTemplatesEnabled()));
            Map<String,String> suiteAllEval = evaluateExpressionMapNow(suite.getBeforeAll(), suite.getBeforeAllEnabled(), run.vars);
            run.vars.fillSuiteVarsFromMap(suiteAllEval);
            run.suiteBeforeAllDone.add(suite.getId());
        }
    }

    private void initCaseSymbols(TestRun run, TestNode node, TestCase testCase) throws Exception {
        if (testCase == null) return; TestSuite parentSuite = resolveParentSuite(node);
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        // Case-Templates zuerst (lazy); danach Before-Kette ausführen
        run.vars.fillCaseTemplatesFromMap(filterEnabled(testCase.getTemplates(), testCase.getTemplatesEnabled()));
        java.util.List<ThrowingRunnable> beforeChain = new java.util.ArrayList<>();
        // Reihenfolge: Root BeforeEach -> Suite BeforeEach -> Case Before (entspricht Top-Down mit All schon erledigt)
        beforeChain.add(() -> run.vars.fillCaseVarsFromMap(evaluateExpressionMapNow(rootModel != null ? rootModel.getBeforeEach() : null, rootModel != null ? rootModel.getBeforeEachEnabled() : null, run.vars)));
        beforeChain.add(() -> run.vars.fillCaseVarsFromMap(evaluateExpressionMapNow(parentSuite != null ? parentSuite.getBeforeEach() : null, parentSuite != null ? parentSuite.getBeforeEachEnabled() : null, run.vars)));
        beforeChain.add(() -> run.vars.fillCaseVarsFromMap(evaluateExpressionMapNow(testCase.getBefore(), testCase.getBeforeEnabled(), run.vars)));
        for (ThrowingRunnable r : beforeChain) { runUnchecked(r); Integer waitCfg = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class); long w = (waitCfg != null) ? waitCfg.longValue() : 0L; if (w > 0) { try { Thread.sleep(w); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } } }
        if (testCase.getId() != null) run.caseBeforeChainDone.add(testCase.getId());
    }

    private void ensureCaseInitForAction(TestRun run, TestNode caseNode, TestCase testCase) throws RuntimeException {
        try {
            TestSuite parentSuite = resolveParentSuite(caseNode);
            if (parentSuite != null) initSuiteSymbols(run, parentSuite);
            if (testCase.getId() == null || !run.caseBeforeChainDone.contains(testCase.getId())) {
                run.vars.enterCase(); initCaseSymbols(run, caseNode, testCase);
            }
        } catch (Exception ex) { throw new RuntimeException("Initialisierung (ad-hoc) fehlgeschlagen: " + safeMsg(ex), ex); }
    }

    private TestSuite resolveParentSuite(TestNode node) { if (node == null) return null; if (node.getParent() instanceof TestNode) { Object pm = ((TestNode) node.getParent()).getModelRef(); if (pm instanceof TestSuite) return (TestSuite) pm; } return null; }

    private String parseIdFromValue(String value) { if (value == null) return ""; String[] pairs = value.split("&"); for (String pair : pairs) { String[] kv = pair.split("=", 2); if (kv.length == 2 && "id".equals(kv[0])) return kv[1]; } return ""; }
    private String resolvePreconditionName(String id) { if (id == null || id.trim().isEmpty()) return "(keine)"; java.util.List<de.bund.zrb.model.Precondition> list = de.bund.zrb.service.PreconditionRegistry.getInstance().getAll(); for (de.bund.zrb.model.Precondition p : list) { if (id.equals(p.getId())) { String n = p.getName(); return (n != null && n.trim().length() > 0) ? n.trim() : "(unnamed)"; } } return id; }

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
            try { page.waitForTimeout(Math.min(to, 1000)); } catch (Throwable ignore2) {}
        }
    }
}
