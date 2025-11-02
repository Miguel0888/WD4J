package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.PageImpl;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
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
        // Neue Case-Variablen beginnen
        runtimeCtx.enterCase();

        // Subtitle = TestCase-Name
        String sub = (testCase.getName() != null) ? testCase.getName().trim() : "";
        OverlayBridge.setSubtitle(sub);

        SuiteLog caseLog = new SuiteLog(testCase.getName());
        logger.append(caseLog);                           // <— Überschrift sofort anzeigen

        // GIVEN (Case) – einzeln streamen
        executeGivenList(testCase.getBefore(), caseLog, "Given");

        // WHEN (Kinder) – rekursiv streamen
        executeChildren(node, caseLog);

        // THEN – streamen (macht Screenshot + Bild einbetten)
        executeThenPhase(node, testCase, caseLog);

        drawerRef.updateSuiteStatus(node);
        return caseLog; // Rückgabe optional, wird nicht mehr am Ende appended
    }

    private LogComponent executeSuiteNode(TestNode node, TestSuite suite) {
        // Beginne neue Suite → Variablen-Kontext für Suite + Case zurücksetzen
        runtimeCtx.enterSuite();

        // Caption = Suite-Description (oder Name), Subtitle leer
        String cap = (suite.getDescription() != null && !suite.getDescription().trim().isEmpty())
                ? suite.getDescription().trim()
                : (suite.getName() != null ? suite.getName().trim() : node.toString());
        OverlayBridge.setCaption(cap);
        OverlayBridge.clearSubtitle();

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
        List<LogComponent> out = new ArrayList<LogComponent>();
        if (givens == null || givens.isEmpty()) return out;

        for (int i = 0; i < givens.size(); i++) {

            GivenCondition given = givens.get(i);

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

                // 1. Führe das Given fachlich aus (z. B. Benutzer anlegen, Session aufbauen, etc.)
                givenExecutor.apply(user, given);

                // 2. Schreibe Variablen ins laufende Case-Scope
                //
                // username ist ein super häufiges Binding -> als caseVar ablegen
                if (user != null && user.trim().length() > 0) {
                    runtimeCtx.setCaseVar("username", user.trim());
                }

                // Falls dein GivenParameterMap weitere Werte enthält, lege sie auch ab:
                // Beispiel: Belegnummer=4711-ABC aus "Es existiert eine {{Belegnummer}}."
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

                // 3. Markiere im Log als Erfolg
                givenLog.setStatus(true);

            } catch (Exception ex) {
                givenLog.setStatus(false);
                givenLog.setError(ex.getMessage());
            }

            givenLog.setParent(parentLog);
            logger.append(givenLog);
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

    public synchronized boolean playSingleAction(final TestAction action, final StepLog stepLog) {
        try {
            // 1. Welcher User führt diese Action aus (Browserkontext)?
            final String effectiveUser = resolveEffectiveUserForAction(action);

            // 2. Merken, damit nachfolgende Actions gleichen User behalten können
            lastUsernameUsed = effectiveUser;

            // 3. Browser-Kontext auf diesen User umschalten
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

            // 4. Stelle sicher, dass username auch wirklich im Case-Scope steht,
            //    falls kein Given vorher gesetzt hat:
            if (effectiveUser != null && effectiveUser.trim().length() > 0) {
                runtimeCtx.setCaseVar("username", effectiveUser.trim());
            }

            // 5. Baue jetzt den ValueScope aus runtimeCtx (case -> suite -> root)
            final ValueScope scopeForThisAction = runtimeCtx.buildCaseScope();

            // 6. Was für eine Aktion ist das?
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

                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
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
                                    // For press: normalerweise feste Keys wie "Enter".
                                    // Wenn du jemals dynamisch pressen willst:
                                    // final String key = resolveActionValueAtRuntime(action, scopeForThisAction);
                                    // loc.press(key);
                                    loc.press(action.getValue());
                                }
                            });
                        }
                    });
                    return true;
                }

                case "type": {
                    final Locator loc = LocatorResolver.resolve(page, action);

                    withRecordingSuppressed(page, new Runnable() {
                        public void run() {
                            waitThen(loc, action.getTimeout(), new Runnable() {
                                public void run() {
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

        } catch (Exception e) {
            System.err.println("❌ Fehler bei Playback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private byte[] screenshotAfterWait(int timeout, PageImpl page) {
//        sleep(3000);
        waitForStableBeforeScreenshot(page, timeout);
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
        // 1) Explizit im Given gesetzt?
        Object u = (given.getParameterMap() != null) ? given.getParameterMap().get("username") : null;
        if (u instanceof String && !((String) u).isEmpty()) {
            return (String) u;
        }
        // 2) Zuletzt in einer Action benutzt?
        if (lastUsernameUsed != null && !lastUsernameUsed.isEmpty()) {
            return lastUsernameUsed;
        }
        // 3) Erster registrierter User (falls vorhanden)
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) {
            return all.get(0).getUsername();
        }
        // 4) Fallback – nur wenn Registry wirklich leer wäre
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
        // reportBaseName=null; reportHtmlPath=null; reportImagesDir=null; screenF"shotCounter=0;
    }

    private void waitForStableBeforeScreenshot(Page page, double timeoutMs) {
        long to = (long) Math.max(1000, timeoutMs);
        page.waitForFunction(
                // *** WICHTIG: Diese Funktion gibt synchron einen BOOLEAN zurück. ***
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
        // nicht gefunden -> zeig die id
        return id;
    }

    /**
     * Resolve the dynamic template for this action into a concrete runtime String.
     *
     * Intent:
     * - Parse placeholders like {{username}} or {{OTP({{username}})}}.
     * - Evaluate functions via ExpressionRegistry.
     * - Evaluate variables via ValueScope chain.
     * - Do this LAZY (right now, not beim Speichern).
     */
    private String resolveActionValueAtRuntime(TestAction action, ValueScope scope) {
        String template = action.getValue(); // z.B. "{{OTP({{username}})}}" oder "4711" oder "{{username}}"
        if (template == null) {
            return "";
        }
        return ActionRuntimeEvaluator.evaluateActionValue(template, scope);
    }

    /**
     * Determine which logical user is active for this action.
     *
     * Intent:
     * - Return the username that should drive browser context AND be exposed
     *   as {{username}} in expressions.
     *
     * Fallback order:
     * 1. action.getUser()
     * 2. lastUsernameUsed (vom vorherigen Step)
     * 3. erster registrierter User aus UserRegistry
     * 4. "default"
     */
    private String resolveEffectiveUserForAction(TestAction action) {
        // 1. Direkt an der Action gesetzt?
        if (action.getUser() != null && action.getUser().trim().length() > 0) {
            return action.getUser().trim();
        }

        // 2. Zuletzt benutzter User im Lauf?
        if (lastUsernameUsed != null && lastUsernameUsed.trim().length() > 0) {
            return lastUsernameUsed.trim();
        }

        // 3. Fallback: nimm ersten bekannten User
        java.util.List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (!all.isEmpty()) {
            UserRegistry.User u = all.get(0);
            if (u != null && u.getUsername() != null && u.getUsername().trim().length() > 0) {
                return u.getUsername().trim();
            }
        }

        // 4. Letzte Eskalationsstufe
        return "default";
    }




}
