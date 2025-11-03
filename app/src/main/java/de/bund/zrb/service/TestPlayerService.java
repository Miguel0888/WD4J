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

        if (stopped) logger.append(new SuiteLog("⏹ Playback abgebrochen!"));

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
            initCaseSymbols(node, testCase); // may throw -> fail case
        } catch (Exception ex) {
            caseLog.setStatus(false);
            caseLog.setError("Case-Setup fehlgeschlagen: " + safeMsg(ex));
            drawerRef.updateSuiteStatus(node);
            return caseLog; // do not run children/then
        }

        String sub = (testCase.getName() != null) ? testCase.getName().trim() : "";
        OverlayBridge.setSubtitle(sub);

        // WHEN – children
        executeChildren(node, caseLog);

        // THEN – final screenshot
        executeThenPhase(node, testCase, caseLog);

        drawerRef.updateSuiteStatus(node);
        return caseLog;
    }

    private void initCaseSymbols(TestNode node, TestCase testCase) throws Exception {
        TestSuite parentSuite = (TestSuite) ((TestNode) node.getParent()).getModelRef();
        RootNode rootModel = TestRegistry.getInstance().getRoot();

        runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(rootModel.getBeforeEach(), runtimeCtx)
        );
        runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(parentSuite.getBeforeEach(), runtimeCtx)
        );
        runtimeCtx.fillCaseVarsFromMap(
                evaluateExpressionMapNow(testCase.getBefore(), runtimeCtx)
        );
        runtimeCtx.fillCaseTemplatesFromMap(testCase.getTemplates());
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
            suiteLog.setError("Suite-Setup fehlgeschlagen: " + safeMsg(ex));
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

    private void initSuiteSymbols(TestSuite suite) throws Exception {
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        if (runtimeCtx.buildCaseScope().lookupVar("___rootInitMarker") == null) {
            Map<String,String> evaluated = evaluateExpressionMapNow(
                    rootModel.getBeforeAll(),
                    runtimeCtx);
            runtimeCtx.fillRootVarsFromMap(evaluated);
            runtimeCtx.fillRootTemplatesFromMap(rootModel.getTemplates());
            runtimeCtx.setRootVar("___rootInitMarker", "done");
        }

        Map<String,String> suiteAllEval = evaluateExpressionMapNow(
                suite.getBeforeAll(),
                runtimeCtx);
        runtimeCtx.fillSuiteVarsFromMap(suiteAllEval);
        runtimeCtx.fillSuiteTemplatesFromMap(suite.getTemplates());
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
                logText = "Precondition: " + name;
            } else {
                logText = "Given: " + given.getType();
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

    private List<LogComponent> executeThenPhase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<LogComponent>();

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
            thenLog.setError("Screenshot fehlgeschlagen: " + safeMsg(ex));
        }

        logger.append(thenLog);
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
        return (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) ? lastUsernameUsed : "default";
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
                        "Kein Tab für den im Testfall eingestellten User verfügbar (" + effectiveUser + ")."
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

                    String baseName = (stepLog.getName() == null) ? "case" : stepLog.getName();
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
                    System.out.println("⚠️ Nicht unterstützte Action: " + act);
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
                ? "shot"
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
            RuntimeVariableContext ctx
    ) throws Exception {
        java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<String,String>();
        if (src == null) return out;

        for (java.util.Map.Entry<String,String> e : src.entrySet()) {
            String key = e.getKey();
            String exprText = e.getValue();

            ValueScope currentScope = ctx.buildCaseScope(); // include already set values

            String resolved = ActionRuntimeEvaluator.evaluateActionValue(exprText, currentScope);
            if (resolved == null) resolved = "";

            out.put(key, resolved);

            // Shadow immediately in case scope for subsequent keys
            ctx.setCaseVar(key, resolved);
        }

        return out;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // ThrowingRunnable-Unterstützung
    ////////////////////////////////////////////////////////////////////////////////

    // Comment: Execute ThrowingRunnable and convert checked exceptions into RuntimeException
    private void runUnchecked(ThrowingRunnable op) {
        try {
            op.run();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new ActionEvaluationRuntimeException(ex);
        }
    }

    // Utility: keep error messages compact
    private String safeMsg(Throwable t) {
        return (t == null) ? "" : (t.getMessage() != null ? t.getMessage() : t.toString());
    }
}
