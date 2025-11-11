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
    private static final String TYPE_PRECONDITION_REF = "preconditionRef";
    // --- Centralized constants (avoid magic literals throughout the class) ---
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

    private final RuntimeVariableContext runtimeCtx =
            new RuntimeVariableContext(ExpressionRegistryImpl.getInstance());

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

    private boolean rootBeforeAllDone = false; // neu: verhindert Doppel-Ausführung
    private final java.util.Set<String> suiteBeforeAllDone = new java.util.HashSet<>(); // neu pro Suite-ID

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
        ActivityService.getInstance(browserService.getBrowser());
        resetRunFlags();
        if (!isReady()) return;

        beginReport();

        TestNode start = resolveStartNode();
        runNodeStepByStep(start);

        if (stopped) logger.append(new SuiteLog(PLAYBACK_ABORTED_MSG));

        OverlayBridge.clearSubtitle();
        OverlayBridge.clearCaption();
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
            return executeGenericContainerNode(node);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Node-Ausführungen
    ////////////////////////////////////////////////////////////////////////////////

    private LogComponent executeActionNode(TestNode node, TestAction action) {
        StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));

        boolean ok;
        String err = null;
        try {
            // Setup minimal case scope if action is run standalone (parent case exists)
            TestNode caseNode = (TestNode) node.getParent();
            if (caseNode != null && caseNode.getModelRef() instanceof TestCase) {
                TestCase tc = (TestCase) caseNode.getModelRef();
                // Clear case-scope and evaluate only Case-level Before and Templates
                runtimeCtx.enterCase();

                // Pre-set username to resolve expressions that use {{user}}
                String effectiveUser = resolveEffectiveUserForAction(action);
                if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                    runtimeCtx.setCaseVar("username", effectiveUser.trim());
                }

                // Evaluate case-level Befores without suite/root
                ValueScope actionOnlyScope = runtimeCtx.buildCaseScopeForActionOnly();
                runUnchecked(new ThrowingRunnable() {
                    @Override public void run() throws Exception {
                        evaluateExpressionMapNowWithScope(tc.getBefore(), tc.getBeforeEnabled(), actionOnlyScope, runtimeCtx);
                    }
                });
                runtimeCtx.fillCaseTemplatesFromMap(filterEnabled(tc.getTemplates(), tc.getTemplatesEnabled()));
            }

            ok = playSingleAction(action, stepLog);
            if (!ok) err = "Action returned false";
        } catch (RuntimeException ex) {
            ok = false;
            err = (ex.getMessage() != null) ? ex.getMessage() : ex.toString();
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

    private LogComponent executeTestCaseNode(TestNode node, TestCase testCase) {
        runtimeCtx.enterCase();

        SuiteLog caseLog = new SuiteLog(testCase.getName());
        logger.append(caseLog); // show header immediately

        try {
            // Ensure suite/root befores/templates are initialized so that BeforeEach has {{user}} available
            TestNode parentNode = (TestNode) node.getParent();
            TestSuite parentSuite = (parentNode != null && parentNode.getModelRef() instanceof TestSuite)
                    ? (TestSuite) parentNode.getModelRef() : null;
            if (parentSuite != null) {
                initSuiteSymbols(parentSuite);
            }

            initCaseSymbols(node, testCase); // may throw -> fail case
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError(CASE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog; // do not run children/then
        }

        // Execute Precondition references (if any) that are attached to Root/Suite/Case.
        // Preconditions run in the Case-scope and may use variables set by BeforeEach/Before.
        try {
            executePreconditionsForCase(node, testCase, caseLog);
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError("Precondition execution failed: " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog; // abort case execution
        }

        String sub = (testCase.getName() != null) ? testCase.getName().trim() : "";
        OverlayBridge.setSubtitle(sub);

        // WHEN – children
        executeChildren(node, caseLog);

        // THEN – final screenshot
        executeThenPhase(node, testCase, caseLog);

        // AFTER – Assertions
        executeAfterAssertions(node, testCase, caseLog);

        drawerRef.updateSuiteStatus(node);
        return caseLog;
    }

    /**
     * Resolve and execute Precondition references attached to Root/Suite/Case.
     * Each Precondition may contain Given entries (Precondtion) and action steps (TestAction).
     * They are executed in the current case scope so they can use the same variables/templates.
     */
    private void executePreconditionsForCase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) throws Exception {
        if (testCase == null) return;

        // Collect Precondition references in order: Root -> Suite -> Case
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
        preLog.setParent(parentLog);
        logger.append(preLog);

        // For each precondition reference, try to resolve to a Precondition object
        for (Precondtion ref : refs) {
            if (ref == null) continue;
            if (!TYPE_PRECONDITION_REF.equals(ref.getType())) {
                // Treat as a normal Given entry
                executeGivenList(java.util.Collections.singletonList(ref), preLog, PRECONDITION_PREFIX);
                continue;
            }

            String id = parseIdFromValue(ref.getValue());
            if (id == null || id.trim().isEmpty()) {
                StepLog err = new StepLog(PRECONDITION_PREFIX, "Unbekannte Precondition-Referenz (keine id)");
                err.setStatus(false);
                err.setParent(preLog);
                logger.append(err);
                continue;
            }

            Precondition p = PreconditionRegistry.getInstance().getById(id);
            String displayName = (p != null) ? (p.getName() != null ? p.getName() : id) : id;

            // 1) Execute 'given' entries of the Precondition (these are Precondtion items)
            if (p != null && p.getGiven() != null && !p.getGiven().isEmpty()) {
                executeGivenList(p.getGiven(), preLog, PRECONDITION_PREFIX + displayName);
            }

            // 2) Execute action steps of the Precondition
            if (p != null && p.getActions() != null && !p.getActions().isEmpty()) {
                for (TestAction pa : p.getActions()) {
                    StepLog stepLog = new StepLog("PRECOND", buildStepText(pa));
                    try {
                        // Ensure username in scope if action has user or fallback
                        String effectiveUser = (pa.getUser() != null && pa.getUser().trim().length() > 0)
                                ? pa.getUser().trim() : resolveUserForTestCase(caseNode);
                        if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                            runtimeCtx.setCaseVar("username", effectiveUser.trim());
                        }
                        boolean ok = playSingleAction(pa, stepLog);
                        stepLog.setStatus(ok);
                        if (!ok) stepLog.setError("Precondition action failed");
                    } catch (Exception ex) {
                        stepLog.setStatus(false);
                        stepLog.setError(safeMsg(ex));
                    }
                    stepLog.setParent(preLog);
                    logger.append(stepLog);
                }
            } else {
                // If Precondition object not found, log a warning
                if (p == null) {
                    StepLog warn = new StepLog(PRECONDITION_PREFIX, "Precondition not found: " + id);
                    warn.setStatus(false);
                    warn.setParent(preLog);
                    logger.append(warn);
                }
            }
        }
    }

    private LogComponent executeSuiteNode(TestNode node, TestSuite suite) {
        runtimeCtx.enterSuite();

        SuiteLog suiteLog = new SuiteLog(node.toString());
        logger.append(suiteLog); // show header immediately

        try {
            initSuiteSymbols(suite); // may throw -> fail suite
        } catch (Exception ex) {
            String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                    ? suite.getDescription().trim()
                    : (suite.getName() != null ? suite.getName().trim() : node.toString());
            OverlayBridge.setCaption(cap);
            OverlayBridge.clearSubtitle();

            suiteLog.setStatus(false);
            suiteLog.setError(SUITE_SETUP_FAILED_MSG + ": " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return suiteLog; // do not run children
        }

        String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                ? suite.getDescription().trim()
                : (suite.getName() != null ? suite.getName().trim() : node.toString());
        OverlayBridge.setCaption(cap);
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
    ////////////////////////////////////////////////////////////////////////////////

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

    private List<LogComponent> executeGivenList(List<Precondtion> givens, SuiteLog parentLog, String label) {
        List<LogComponent> out = new ArrayList<LogComponent>();
        if (givens == null || givens.isEmpty()) return out;

        for (int i = 0; i < givens.size(); i++) {
            Precondtion given = givens.get(i);

            String logText;
            if (TYPE_PRECONDITION_REF.equals(given.getType())) {
                String id = parseIdFromValue(given.getValue());
                String name = resolvePreconditionName(id);
                logText = PRECONDITION_PREFIX + name;
            } else {
                logText = GIVEN_PREFIX + given.getType();
            }

            StepLog givenLog = new StepLog(label, logText);

            try {
                String user = inferUsername(given);

                // 1. Execute business precondition
                givenExecutor.apply(user, given);

                // 2. Reflect values into case scope
                if (user != null && user.trim().length() > 0) {
                    runtimeCtx.setCaseVar("username", user.trim());
                }
                if (given.getParameterMap() != null) {
                    java.util.Map<String, Object> p = given.getParameterMap();
                    for (java.util.Map.Entry<String, Object> entry : p.entrySet()) {
                        String k = entry.getKey();
                        Object v = entry.getValue();
                        if (k != null && v instanceof String && ((String) v).trim().length() > 0) {
                            runtimeCtx.setCaseVar(k, ((String) v).trim());
                        }
                    }
                }

                givenLog.setStatus(true);

            } catch (Exception ex) {
                givenLog.setStatus(false);
                givenLog.setError(safeMsg(ex));
            }

            givenLog.setParent(parentLog);
            logger.append(givenLog);
            out.add(givenLog);
        }

        return out;
    }

    @Deprecated // use after
    private List<LogComponent> executeThenPhase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<LogComponent>();

//        StepLog thenLog = new StepLog("THEN", "Screenshot am Ende des TestCase");
//        thenLog.setParent(parentLog);
//
//        try {
//            String username = resolveUserForTestCase(caseNode);
//            PageImpl page = (PageImpl) browserService.getActivePage(username);
//
//            byte[] png = screenshotAfterWait(3000, page);
//
//            String baseName = (testCase.getName() == null) ? "case" : testCase.getName();
//            Path file = saveScreenshotBytes(png, baseName);
//            String rel = relToHtml(file);
//
//            thenLog.setStatus(true);
//            thenLog.setHtmlAppend(
//                    "<img src='" + rel + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>"
//            );
//        } catch (Exception ex) {
//            thenLog.setStatus(false);
//            thenLog.setError("Screenshot fehlgeschlagen: " + safeMsg(ex));
//        }
//
//        logger.append(thenLog);
//        out.add(thenLog);
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
        return (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) ? lastUsernameUsed : DEFAULT_USERNAME;
    }

    // --- Public API for tools: save screenshot into report and return relative path (for <img src=...>) ---
    public String saveScreenshotFromTool(byte[] png, String baseName) throws Exception {
        initReportIfNeeded();
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = SCREENSHOT_TOOL_BASE;
        }
        Path file = saveScreenshotBytes(png, baseName);
        return relToHtml(file);
    }

    /**
     * Append a single log step with an <img> to the current logger.
     * Keep it lightweight; tools can call this after saveScreenshotFromTool.
     */
    public void logScreenshotFromTool(String label, String relImagePath, boolean ok, String errorMsg) {
        StepLog log = new StepLog(LOG_LABEL_ASSERT, (label == null || label.trim().isEmpty()) ? SCREENSHOT_LABEL : label);
        log.setStatus(ok);
        if (!ok && errorMsg != null && errorMsg.trim().length() > 0) {
            log.setError(errorMsg.trim());
        }
        if (relImagePath != null && relImagePath.trim().length() > 0) {
            log.setHtmlAppend("<img src='" + relImagePath + "' alt='Screenshot' " +
                    "style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>");
        }
        if (logger != null) {
            logger.append(log);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Aktion ausführen
    ////////////////////////////////////////////////////////////////////////////////

    public synchronized boolean playSingleAction(final TestAction action, final StepLog stepLog) {
        try {
            // 1) User-Kontext
            final String effectiveUser = resolveEffectiveUserForAction(action);
            lastUsernameUsed = effectiveUser;

            // 2) Browser-Kontext
            PageImpl page = (PageImpl) browserService.getActivePage(effectiveUser);
            if (page != null) {
                String contextId = page.getBrowsingContext().value();
                browserService.switchSelectedPage(contextId);
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        String.format(NO_TAB_FOR_USER_MSG, effectiveUser)
                );
                return false;
            }

            // 3) username in Case-Scope
            if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                runtimeCtx.setCaseVar("username", effectiveUser.trim());
            }

            // 4) Scope
            final ValueScope scopeForThisAction = runtimeCtx.buildCaseScope();

            // 5) Action
            String act = action.getAction();

            switch (act) {

                case "navigate": {
                    final String navUrl = resolveActionValueAtRuntime(action, scopeForThisAction);
                    return doWithSettling(page, action.getTimeout(), new Runnable() {
                        public void run() {
                            page.navigate(
                                    navUrl,
                                    new Page.NavigateOptions().setTimeout(action.getTimeout())
                            );
                        }
                    });
                }

                case "wait":
                    Thread.sleep(Long.parseLong(action.getValue()));
                    return true;

                case "click": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
                                    doWithSettling(page, action.getTimeout(), new Runnable() {
                                        public void run() {
                                            loc.click(new Locator.ClickOptions().setTimeout(action.getTimeout()));
                                        }
                                    });
                                }
                            });
                        }
                    });
                    return true;
                }

                case "input":
                case "fill": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() {
                        public void run() throws Exception {
                            waitThen(loc, action.getTimeout(), new ThrowingRunnable() {
                                public void run() throws Exception {
                                    // Evaluate just-in-time (time-sensitive)
                                    final String resolvedText =
                                            resolveActionValueAtRuntime(action, scopeForThisAction);
                                    loc.fill(
                                            resolvedText,
                                            new Locator.FillOptions().setTimeout(action.getTimeout())
                                    );
                                }
                            });
                        }
                    });
                    return true;
                }

                case "select": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    final String optionToSelect =
                            resolveActionValueAtRuntime(action, scopeForThisAction);

                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
                                    loc.selectOption(optionToSelect);
                                }
                            });
                        }
                    });
                    return true;
                }

                case "check":
                case "radio": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
                                    loc.check(new Locator.CheckOptions().setTimeout(action.getTimeout()));
                                }
                            });
                        }
                    });
                    return true;
                }

                case "screenshot": {
                    byte[] png = screenshotAfterWait(action.getTimeout(), page);

                    String baseName = (stepLog.getName() == null) ? SCREENSHOT_CASE_BASE : stepLog.getName();
                    Path file = saveScreenshotBytes(png, baseName);
                    String rel = relToHtml(file);

                    stepLog.setStatus(true);
                    stepLog.setHtmlAppend(
                            "<img src='" + rel + "' alt='Screenshot' style='max-width:100%;border:1px solid #ccc;margin-top:.5rem'/>"
                    );
                    return true;
                }

                case "press": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
                                    loc.press(action.getValue());
                                }
                            });
                        }
                    });
                    return true;
                }

                case "type": {
                    final Locator loc = LocatorResolver.resolve(page, action);
                    withRecordingSuppressed(page, new ThrowingRunnable() {
                        public void run() throws Exception {
                            waitThen(loc, action.getTimeout(), new ThrowingRunnable() {
                                public void run() throws Exception {
                                    // Evaluate just-in-time (time-sensitive)
                                    final String resolvedText =
                                            resolveActionValueAtRuntime(action, scopeForThisAction);
                                    loc.type(resolvedText);
                                }
                            });
                        }
                    });
                    return true;
                }

                default:
                    System.out.println(String.format(UNSUPPORTED_ACTION_MSG, act));
                    return false;
            }

         } catch (ActionEvaluationRuntimeException aerx) {
            Throwable cause = (aerx.getCause() != null) ? aerx.getCause() : aerx;
            System.err.println("❌ Evaluation failed: " + cause.getMessage());
            cause.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ Fehler bei Playback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private byte[] screenshotAfterWait(int timeout, PageImpl page) {
        waitForStableBeforeScreenshot(page, timeout);
        return page.screenshot(new Page.ScreenshotOptions().setTimeout(timeout));
    }

    // --- Overload 1: Runnable (bestehend) ---
    private void waitThen(Locator locator, double timeout, Runnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        action.run();
    }

    // --- Overload 2: ThrowingRunnable (neu) ---
    private void waitThen(Locator locator, double timeout, ThrowingRunnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        runUnchecked(action);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Logging/Hilfen
    ////////////////////////////////////////////////////////////////////////////////

    private StepLog buildStepLogForAction(TestAction action) {
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

    private String inferUsername(Precondtion given) {
        Object u = (given.getParameterMap() != null) ? given.getParameterMap().get("username") : null;
        if (u instanceof String && !((String) u).isEmpty()) return (String) u;
        if (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) return lastUsernameUsed;
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) return all.get(0).getUsername();
        return "default";
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
                ? SCREENSHOT_SHOT_BASE
                : baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = String.format("%03d-%s.png", ++screenshotCounter, safe);
        Path file = reportImagesDir.resolve(fileName);
        Files.write(file, png);
        return file;
    }

    // --- Overload 1: Runnable (bestehend) ---
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

    // --- Overload 2: ThrowingRunnable (neu) ---
    private void withRecordingSuppressed(Page page, ThrowingRunnable action) {
        try {
            page.evaluate("() => { window.__zrbSuppressRecording = (window.__zrbSuppressRecording || 0) + 1; }");
        } catch (Throwable ignore) {}
        try {
            runUnchecked(action);
        } finally {
            try {
                page.evaluate("() => { window.__zrbSuppressRecording = Math.max((window.__zrbSuppressRecording || 1) - 1, 0); }");
            } catch (Throwable ignore) {}
        }
    }

    // Helpers:
    private void beginReport() {
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
            logger.setDocumentBase(baseDir);
        }
    }

    private void initReportIfNeeded() {
        if (reportBaseName != null) return;

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
        Path base = reportHtmlPath.getParent();
        String rel = base.relativize(file).toString();
        return rel.replace('\\', '/');
    }

    private void endReport() {
        if (logger != null) {
            logger.exportAsHtml(reportHtmlPath);
        }
    }

    private void waitForStableBeforeScreenshot(Page page, double timeoutMs) {
        long to = (long) Math.max(1000, timeoutMs);
        page.waitForFunction(
                "quietMs => {"
                        + "  try {"
                        + "    const getA = (typeof window.__zrbGetActivity === 'function') ? window.__zrbGetActivity : null;"
                        + "    const now  = Date.now();"
                        + "    if (getA) {"
                        + "      const a = getA() || {};"
                        + "      const inflight = (a.inflightXHR|0) + (a.inflightFetch|0) + (a.pfQueueDepth|0);"
                        + "      const last = a.lastChangeTs || 0;"
                        + "      return inflight === 0 && (now - last) >= quietMs;"
                        + "    }"
                        + "    return false;"
                        + "  } catch(_) {"
                        + "    return false;"
                        + "  }"
                        + "}",
                QUIET_MS,
                new Page.WaitForFunctionOptions().setTimeout(to)
        );
    }

    /** Erwartet "id=<uuid>&..." im value. */
    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] pairs = value.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) return kv[1];
        }
        return "";
    }

    /** Liefert Anzeigenamen der Precondition (Fallback: UUID). */
    private String resolvePreconditionName(String id) {
        if (id == null || id.trim().isEmpty()) return "(keine)";
        java.util.List<de.bund.zrb.model.Precondition> list =
                de.bund.zrb.service.PreconditionRegistry.getInstance().getAll();
        for (de.bund.zrb.model.Precondition p : list) {
            if (id.equals(p.getId())) {
                String n = p.getName();
                return (n != null && n.trim().length() > 0) ? n.trim() : "(unnamed)";
            }
        }
        return id;
    }

    /**
     * Resolve the dynamic template for this action into a concrete runtime String.
     */
    private String resolveActionValueAtRuntime(TestAction action, ValueScope scope) throws Exception {
        String template = action.getValue();
        if (template == null) return "";
        return ActionRuntimeEvaluator.evaluateActionValue(template, scope);
    }

    /**
     * Determine which logical user is active for this action.
     */
    private String resolveEffectiveUserForAction(TestAction action) {
        if (action.getUser() != null && action.getUser().trim().length() > 0) return action.getUser().trim();
        if (lastUsernameUsed != null && lastUsernameUsed.trim().length() > 0) return lastUsernameUsed.trim();
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) {
            UserRegistry.User u = all.get(0);
            if (u != null && u.getUsername() != null && u.getUsername().trim().length() > 0) {
                return u.getUsername().trim();
            }
        }
        return "default";
    }

    /**
     * Werte eine Map<String,String> sofort aus; werfe checked Exception, um Case/Suite scheitern zu lassen.
     */
    private Map<String,String> evaluateExpressionMapNow(
            Map<String,String> src,
            Map<String, Boolean> enabled,
            RuntimeVariableContext ctx
    ) throws Exception {
        java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<String,String>();
        if (src == null) return out;

        for (java.util.Map.Entry<String,String> e : src.entrySet()) {
            String key = e.getKey();
            String exprText = e.getValue();
            if (!isEnabled(enabled, key)) continue;

            ValueScope currentScope = ctx.buildCaseScope(); // include already set values

            String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, currentScope);
            if (resolved == null) resolved = "";

            out.put(key, resolved);

            // Shadow immediately in case scope for subsequent keys
            ctx.setCaseVar(key, resolved);
        }

        return out;
    }

    private Map<String,String> evaluateExpressionMapNowWithScope(
            Map<String,String> src,
            Map<String, Boolean> enabled,
            ValueScope scope,
            RuntimeVariableContext ctx
    ) throws Exception {
        java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<String,String>();
        if (src == null) return out;

        for (java.util.Map.Entry<String,String> e : src.entrySet()) {
            String key = e.getKey();
            String exprText = e.getValue();
            if (!isEnabled(enabled, key)) continue;

            String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, scope);
            if (resolved == null) resolved = "";

            out.put(key, resolved);

            // Shadow immediately in case scope for subsequent keys
            ctx.setCaseVar(key, resolved);
        }

        return out;
    }

    private Map<String,String> filterEnabled(Map<String,String> src, Map<String, Boolean> enabled) {
        java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<String,String>();
        if (src == null) return out;
        for (java.util.Map.Entry<String,String> e : src.entrySet()) {
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

    // Comment: Execute After/Expectation assertions with optional human-readable descriptions.
// - PASS: null, "", or "true" (case-insensitive)
// - FAIL: anything else -> use the string itself as error message (no prefix)
// - Display text: prefer description (if present), otherwise short expression.
    private List<LogComponent> executeAfterAssertions(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<LogComponent>();
        try {
            final ValueScope scope = runtimeCtx.buildCaseScope();
            RootNode root = TestRegistry.getInstance().getRoot();
            TestSuite suite = null;
            if (caseNode.getParent() instanceof TestNode) {
                Object parentModel = ((TestNode) caseNode.getParent()).getModelRef();
                if (parentModel instanceof TestSuite) {
                    suite = (TestSuite) parentModel;
                }
            }

            java.util.LinkedHashMap<String,String> collected = new java.util.LinkedHashMap<String,String>();
            java.util.LinkedHashMap<String,String> descriptions = new java.util.LinkedHashMap<String,String>();
            // NEW: validator type/value maps parallel
            java.util.LinkedHashMap<String,String> validatorTypes = new java.util.LinkedHashMap<String,String>();
            java.util.LinkedHashMap<String,String> validatorValues = new java.util.LinkedHashMap<String,String>();

            if (root != null && root.getAfterEach() != null && root.getAfterEachEnabled() != null) {
                Map<String,String> descMap = safeMap(root.getAfterEachDesc());
                Map<String,String> vtMap = safeMap(root.getAfterEachValidatorType());
                Map<String,String> vvMap = safeMap(root.getAfterEachValidatorValue());
                for (java.util.Map.Entry<String,String> e : root.getAfterEach().entrySet()) {
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
                Map<String,String> descMap = safeMap(suite.getAfterAllDesc());
                Map<String,String> vtMap = safeMap(suite.getAfterAllValidatorType());
                Map<String,String> vvMap = safeMap(suite.getAfterAllValidatorValue());
                for (java.util.Map.Entry<String,String> e : suite.getAfterAll().entrySet()) {
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
                Map<String,String> descMap = safeMap(testCase.getAfterDesc());
                Map<String,String> vtMap = safeMap(testCase.getAfterValidatorType());
                Map<String,String> vvMap = safeMap(testCase.getAfterValidatorValue());
                for (java.util.Map.Entry<String,String> e : testCase.getAfter().entrySet()) {
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

            SuiteLog afterLog = new SuiteLog(LOG_LABEL_AFTER);
            afterLog.setParent(parentLog);
            logger.append(afterLog);
            out.add(afterLog);

            Integer globalCfg = SettingsService.getInstance().get("assertion.groupWaitMs", Integer.class);
            long globalWaitMs = (globalCfg != null) ? globalCfg.longValue() : DEFAULT_ASSERT_GROUP_WAIT_MS;
            if (globalWaitMs > 0) {
                try { Thread.sleep(globalWaitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            for (java.util.Map.Entry<String,String> e : collected.entrySet()) {
                final String name = e.getKey();
                final String expr = e.getValue();
                final String desc = descriptions.get(name);
                final String displayText = (desc != null && desc.length() > 0)
                        ? (name + " → " + desc)
                        : (name + " → " + shortExpr(expr));

                StepLog assertionLog = new StepLog(LOG_LABEL_EXPECT, displayText);
                assertionLog.setParent(afterLog);

                try {
                    Integer eachCfg = SettingsService.getInstance().get("assertion.eachWaitMs", Integer.class);
                    long waitMs = (eachCfg != null) ? eachCfg.longValue() : DEFAULT_ASSERT_EACH_WAIT_MS;
                    if (waitMs > 0) {
                        try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                    String result = ActionRuntimeEvaluator.evaluateActionValue(expr, scope);
                    String trimmed = (result == null) ? null : result.trim();

                    String vType = validatorTypes.get(name); // may be null
                    String vVal  = validatorValues.get(name); // may be null

                    boolean ok;
                    String errorMsg = null;

                    if (isBlank(vType)) {
                        ok = true;
                        if (trimmed != null && trimmed.length() > 0) {
                            assertionLog.setHtmlAppend("<small style='color:#666'>Wert: " + trimmed.replace("<","&lt;").replace(">","&gt;") + "</small>");
                        }
                    } else {
                        ok = validateValue(vType, vVal, trimmed);
                        if (!ok) {
                            errorMsg = "Validation failed (" + vType + ")" + (trimmed != null ? ": " + trimmed : " (null)");
                        }
                    }

                    assertionLog.setStatus(ok);
                    if (!ok && errorMsg != null) {
                        assertionLog.setError(errorMsg);
                    }
                } catch (Exception ex) {
                    assertionLog.setStatus(false);
                    assertionLog.setError("Exception: " + safeMsg(ex));
                }

                logger.append(assertionLog);
                out.add(assertionLog);
            }

        } catch (Exception ex) {
            StepLog err = new StepLog(LOG_LABEL_AFTER, AFTER_ASSERTIONS_FAILED_MSG);
            err.setStatus(false);
            err.setError(safeMsg(ex));
            err.setParent(parentLog);
            logger.append(err);
            out.add(err);
        }

        return out;
    }

    private boolean validateValue(String type, String expected, String actual) {
        if (type == null) type = "";
        boolean negate = false;
        if (type.startsWith("!")) { negate = true; type = type.substring(1); }
        String t = type.trim().toLowerCase();
        String exp = expected == null ? "" : expected.trim();
        String act = actual == null ? "" : actual.trim();

        // Alternativen aufsplitten (||)
        String[] alternatives = exp.contains("||") ? exp.split("\\|\\|") : new String[]{exp};
        boolean any = false;
        for (String altRaw : alternatives) {
            String alt = altRaw.trim();
            // Leeres Pattern: nur für 'regex' automatisch PASS, nicht für 'fullregex'
            if (alt.isEmpty() && t.equals("regex")) {
                any = true;
                break;
            }
            boolean single;
            switch (t) {
                case "regex": {
                    try { single = java.util.regex.Pattern.compile(alt).matcher(act).find(); } catch (Exception ignore) { single = false; }
                    break; }
                case "fullregex": {
                    if (alt.isEmpty()) { single = act.isEmpty(); } else { try { single = java.util.regex.Pattern.compile(alt).matcher(act).matches(); } catch (Exception ignore) { single = false; } }
                    break; }
                case "contains": single = act.contains(alt); break;
                case "equals": single = act.equals(alt); break;
                case "starts": single = act.startsWith(alt); break;
                case "ends": single = act.endsWith(alt); break;
                case "range": {
                    try {
                        String[] parts = alt.split(":", 2);
                        double min = Double.parseDouble(parts[0]);
                        double max = Double.parseDouble(parts[1]);
                        double val = Double.parseDouble(act);
                        single = val >= min && val <= max;
                    } catch (Exception ignore) { single = false; }
                    break; }
                case "len": {
                    try {
                        if (alt.startsWith(">=")) {
                            int n = Integer.parseInt(alt.substring(2));
                            single = act.length() >= n;
                        } else if (alt.startsWith("<=")) {
                            int n = Integer.parseInt(alt.substring(2));
                            single = act.length() <= n;
                        } else {
                            int n = Integer.parseInt(alt);
                            single = act.length() == n;
                        }
                    } catch (Exception ignore) { single = false; }
                    break; }
                default: single = false; // unbekannt
            }
            if (single) { any = true; break; }
        }
        boolean result = any;
        if (negate) result = !result;
        return result;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String trimToNull(String s) { if (s == null) return null; String t = s.trim(); return t.isEmpty() ? null : t; }
    private String shortExpr(String expr) { if (expr == null) return ""; String t = expr.trim(); return (t.length() <= 120) ? t : t.substring(0,117)+"..."; }
    private static Map<String,String> safeMap(Map<String,String> m) { return (m != null) ? m : java.util.Collections.<String,String>emptyMap(); }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
    private void runUnchecked(ThrowingRunnable op) { try { op.run(); } catch (RuntimeException re) { throw re; } catch (Exception ex) { throw new ActionEvaluationRuntimeException(ex); } }
    private String safeMsg(Throwable t) { return (t == null) ? "" : (t.getMessage() != null ? t.getMessage() : t.toString()); }
    // Test-Hook: Initialisiert Case-Scope ohne vollständige Ausführung.
    void _testInitCaseScope() { runtimeCtx.enterCase(); }

    private void initSuiteSymbols(TestSuite suite) throws Exception {
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        if (!rootBeforeAllDone) {
            Map<String,String> evaluated = evaluateExpressionMapNow(

                    rootModel.getBeforeAll(),
                    rootModel.getBeforeAllEnabled(),
                    runtimeCtx);
            runtimeCtx.fillRootVarsFromMap(evaluated);
            runtimeCtx.fillRootTemplatesFromMap(filterEnabled(rootModel.getTemplates(), rootModel.getTemplatesEnabled()));
            rootBeforeAllDone = true;
        }
        if (suite != null && suite.getId() != null && !suiteBeforeAllDone.contains(suite.getId())) {
            Map<String,String> suiteAllEval = evaluateExpressionMapNow(
                    suite.getBeforeAll(),
                    suite.getBeforeAllEnabled(),
                    runtimeCtx);
            runtimeCtx.fillSuiteVarsFromMap(suiteAllEval);
            runtimeCtx.fillSuiteTemplatesFromMap(filterEnabled(suite.getTemplates(), suite.getTemplatesEnabled()));
            suiteBeforeAllDone.add(suite.getId());
        }
    }

    private void initCaseSymbols(TestNode node, TestCase testCase) throws Exception {
        TestSuite parentSuite = (TestSuite) ((TestNode) node.getParent()).getModelRef();
        RootNode rootModel = TestRegistry.getInstance().getRoot();

        // Reihenfolge: Root.BeforeEach -> Suite.BeforeEach -> Case.Before
        java.util.List<ThrowingRunnable> beforeChain = new java.util.ArrayList<>();
        beforeChain.add(() -> runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(rootModel.getBeforeEach(), rootModel.getBeforeEachEnabled(), runtimeCtx)));
        beforeChain.add(() -> runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(parentSuite != null ? parentSuite.getBeforeEach() : null,
                        parentSuite != null ? parentSuite.getBeforeEachEnabled() : null,
                        runtimeCtx)));
        beforeChain.add(() -> runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(testCase.getBefore(), testCase.getBeforeEnabled(), runtimeCtx)));

        for (ThrowingRunnable r : beforeChain) {
            runUnchecked(r);
            // Wartezeit nach jeder Gruppe (Root/Suite/Case)
            Integer waitCfg = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class);
            long w = (waitCfg != null) ? waitCfg.longValue() : 0L;
            if (w > 0) {
                try { Thread.sleep(w); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        runtimeCtx.fillCaseTemplatesFromMap(filterEnabled(testCase.getTemplates(), testCase.getTemplatesEnabled()));
    }
}
