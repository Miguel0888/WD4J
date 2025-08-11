package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TestPlayerService {

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
        resetRunFlags();
        if (!isReady()) return;

        TestNode start = resolveStartNode();
        LogComponent rootLog = runNodeStepByStep(start);
        if (rootLog != null) logger.append(rootLog);

        if (stopped) logger.append(new SuiteLog("⏹ Playback abgebrochen!"));
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
        playSingleAction(action); // bestehendes Verhalten beibehalten

        StepLog stepLog = buildStepLogForAction(action);
        drawerRef.updateNodeStatus(node, true);
        return stepLog;
    }

    private LogComponent executeTestCaseNode(TestNode node, TestCase testCase) {
        SuiteLog caseLog = new SuiteLog(testCase.getName());
        List<LogComponent> children = new ArrayList<>();

        // GIVEN (Case)
        children.addAll(executeGivenList(testCase.getGiven(), caseLog, "Given"));

        // WHEN (Kinder des TestCase)
        children.addAll(executeChildren(node, caseLog));

        // THEN (Case) – Screenshot am Ende des TestCase
        children.addAll(executeThenPhase(node, testCase, caseLog)); // <- HIER: node übergeben!

        caseLog.setChildren(children);
        drawerRef.updateSuiteStatus(node);
        return caseLog;
    }

    private LogComponent executeSuiteNode(TestNode node, TestSuite suite) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        List<LogComponent> children = new ArrayList<>();

        // GIVEN (Suite)
        children.addAll(executeGivenList(suite.getGiven(), suiteLog, "Suite-Given"));

        // REKURSIV Kinder
        children.addAll(executeChildren(node, suiteLog));

        suiteLog.setChildren(children);
        drawerRef.updateSuiteStatus(node);
        return suiteLog;
    }

    private LogComponent executeGenericContainerNode(TestNode node) {
        SuiteLog suiteLog = new SuiteLog(node.toString());
        List<LogComponent> children = new ArrayList<>(executeChildren(node, suiteLog));
        suiteLog.setChildren(children);
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
            out.add(givenLog);
        }
        return out;
    }

    private List<LogComponent> executeThenPhase(TestNode caseNode, TestCase testCase, SuiteLog parentLog) {
        List<LogComponent> out = new ArrayList<>();

        String content = "Screenshot am Ende des TestCase"; // Default-Text
        boolean ok = true;
        String error = null;

        try {
            String username = resolveUserForTestCase(caseNode);
            Page page = browserService.getActivePage(username);

            byte[] png = page.screenshot(new Page.ScreenshotOptions()); // BiDi-Call

            Path dir = Paths.get("reports");
            Files.createDirectories(dir);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeName = testCase.getName() == null ? "case" : testCase.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = dir.resolve(ts + "-" + safeName + ".png");
            Files.write(file, png);

            content = "Screenshot gespeichert: " + file.toAbsolutePath();
        } catch (Exception ex) {
            ok = false;
            error = "Screenshot fehlgeschlagen: " + ex.getMessage();
        }

        StepLog thenLog = new StepLog("THEN", content);
        thenLog.setParent(parentLog);
        thenLog.setStatus(ok);
        if (!ok) thenLog.setError(error);

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

    public boolean playSingleAction(TestAction action) {
        try {
            lastUsernameUsed = action.getUser();
            if (lastUsernameUsed == null || lastUsernameUsed.isEmpty()) {
                System.err.println("⚠️ Keine User-Zuordnung für Action: " + action.getAction());
                return false;
            }

            Page page = browserService.getActivePage(lastUsernameUsed);
            String act = action.getAction();

            switch (act) {
                case "navigate":
                    page.navigate(action.getValue(),
                            new Page.NavigateOptions().setTimeout(action.getTimeout()));
                    return true;

                case "wait":
                    Thread.sleep(Long.parseLong(action.getValue()));
                    return true;

                case "click": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    waitThen(loc, action.getTimeout(), () ->
                            loc.click(new Locator.ClickOptions().setTimeout(action.getTimeout())));
                    return true;
                }

                case "input":
                case "fill": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    waitThen(loc, action.getTimeout(), () ->
                            loc.fill(action.getValue(), new Locator.FillOptions().setTimeout(action.getTimeout())));
                    return true;
                }

                case "select": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    waitThen(loc, action.getTimeout(), () -> loc.selectOption(action.getValue()));
                    return true;
                }

                case "check":
                case "radio": {
                    Locator loc = LocatorResolver.resolve(page, action);
                    waitThen(loc, action.getTimeout(), () ->
                            loc.check(new Locator.CheckOptions().setTimeout(action.getTimeout())));
                    return true;
                }

                case "screenshot":
                    // Element- oder Page-Screenshot könnte hier später unterschieden werden.
                    page.screenshot(new Page.ScreenshotOptions().setTimeout(action.getTimeout()));
                    return true;

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

    private void waitThen(Locator locator, double timeout, Runnable action) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        action.run();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Logging/Hilfen
    ////////////////////////////////////////////////////////////////////////////////

    private StepLog buildStepLogForAction(TestAction action) {
        StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));
        stepLog.setStatus(true);
        return stepLog;
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
}
