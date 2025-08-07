package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.*;

import java.util.ArrayList;
import java.util.List;

public class TestPlayerService {

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final GivenConditionExecutor givenExecutor = new GivenConditionExecutor();

    private TestPlayerUi drawerRef;
    private TestExecutionLogger logger;

    private volatile boolean stopped = false;
    private volatile boolean running = false;

    private TestPlayerService() {}

    public static TestPlayerService getInstance() {
        return INSTANCE;
    }

    public void registerDrawer(TestPlayerUi playerUi) {
        this.drawerRef = playerUi;
    }

    public void registerLogger(TestExecutionLogger logger) {
        this.logger = logger;
    }

    public void stopPlayback() {
        stopped = true;
    }

    public void runSuites() {
        stopped = false;
        running = true;
        if (drawerRef == null || logger == null) return;

        TestNode node = drawerRef.getSelectedNode();
        if (node == null) node = drawerRef.getRootNode();

        LogComponent rootLog = runNodeStepByStep(node);
        if (rootLog != null) {
            logger.append(rootLog); // Nur einmal hinzufügen – inkl. aller Kinder
        }

        if (stopped) {
            logger.append(new SuiteLog("⏹ Playback abgebrochen!"));
        }
    }

    private LogComponent runNodeStepByStep(TestNode node) {
        if (stopped) return null;

        Object model = node.getModelRef();

        // 💡 Action → StepLog
        if (model instanceof TestAction) {
            TestAction action = (TestAction) model;
            playSingleAction(action);

            StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));
            drawerRef.updateNodeStatus(node, true); // z. B. farbige Markierung
            return stepLog;
        }

        // TestCase mit Given-Blöcken
        if (model instanceof TestCase) {
            TestCase testCase = (TestCase) model;
            SuiteLog suiteLog = new SuiteLog(testCase.getName());

            List<LogComponent> children = new ArrayList<>();

            // Execute Given
            for (GivenCondition given : testCase.getGiven()) {
                StepLog givenLog = new StepLog("Given", "Given: " + given.getType());
                try {
                    // Username aus Parametern, wenn vorhanden
                    String user = (String) given.getParameterMap().get("username");
                    if (user == null || user.isEmpty()) {
                        user = "default"; // Optional: Fallback
                    }
                    givenExecutor.apply(user, given);
                } catch (Exception ex) {
                    givenLog.setStatus(false);
                    givenLog.setError(ex.getMessage());
                }
                givenLog.setParent(suiteLog);
                children.add(givenLog);
            }

            // Rekursiv alle Schritte
            for (int i = 0; i < node.getChildCount(); i++) {
                LogComponent child = runNodeStepByStep((TestNode) node.getChildAt(i));
                if (child != null) {
                    child.setParent(suiteLog);
                    children.add(child);
                }
            }

            suiteLog.setChildren(children);
            drawerRef.updateSuiteStatus(node);
            return suiteLog;
        }

        // Alle anderen Nodes (z. B. Suite)
        SuiteLog suiteLog = new SuiteLog(node.toString());
        List<LogComponent> children = new ArrayList<>();

        for (int i = 0; i < node.getChildCount(); i++) {
            LogComponent child = runNodeStepByStep((TestNode) node.getChildAt(i));
            if (child != null) {
                child.setParent(suiteLog);
                children.add(child);
            }
        }

        suiteLog.setChildren(children);
        drawerRef.updateSuiteStatus(node);
        return suiteLog;
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

    public boolean playSingleAction(TestAction action) {
        try {
            String username = action.getUser();
            if (username == null || username.isEmpty()) {
                System.err.println("⚠️ Keine User-Zuordnung für Action: " + action.getAction());
                return false;
            }

            Page page = browserService.getActivePage(username);

            switch (action.getAction()) {
                case "navigate":
                    page.navigate(action.getValue(), new Page.NavigateOptions().setTimeout(action.getTimeout()));
                    break;
                case "click":
                    Locator clickLocator = page.locator(action.getSelectedSelector());
                    clickLocator.waitFor(new Locator.WaitForOptions().setTimeout(action.getTimeout()));
                    clickLocator.click(new Locator.ClickOptions().setTimeout(action.getTimeout()));
                    break;
                case "input":
                case "fill":
                    Locator fillLocator = page.locator(action.getSelectedSelector());
                    fillLocator.waitFor(new Locator.WaitForOptions().setTimeout(action.getTimeout()));
                    fillLocator.fill(action.getValue(), new Locator.FillOptions().setTimeout(action.getTimeout()));
                    break;
                case "wait":
                    long waitTime = Long.parseLong(action.getValue());
                    Thread.sleep(waitTime);
                    break;
                default:
                    System.out.println("⚠️ Nicht unterstützte Action: " + action.getAction());
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Fehler bei Playback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
