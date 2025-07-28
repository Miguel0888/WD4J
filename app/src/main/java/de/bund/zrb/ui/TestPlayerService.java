package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.StepLog;
import de.bund.zrb.ui.components.log.SuiteLog;
import de.bund.zrb.ui.components.log.TestCaseLog;
import de.bund.zrb.ui.components.log.TestExecutionLogger;

import javax.swing.*;
import java.util.List;

public class TestPlayerService {

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private TestPlayerUi drawerRef;
    private TestExecutionLogger logger;

    private SuiteLog currentSuiteLog;
    private TestCaseLog currentTestCaseLog;

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

    public void runSuites() {
        if (drawerRef == null) {
            System.err.println("⚠️ Kein Drawer registriert!");
            return;
        }
        if (logger == null) {
            System.err.println("⚠️ Kein Logger registriert!");
            return;
        }

        TestNode node = drawerRef.getSelectedNode();
        if (node == null) {
            node = drawerRef.getRootNode(); // ✅ Wenn nix markiert ist → Root
        }

        String suiteName = node.toString();
        currentSuiteLog = new SuiteLog(suiteName);
        runNodeRecursive(node);
        logger.append(currentSuiteLog);
    }

    private void runNodeRecursive(TestNode node) {
        if (node.getChildCount() == 0) {
            // Blattknoten: Action ausführen und loggen
            boolean passed = playLeafAction(node);
            drawerRef.updateNodeStatus(node, passed);
            return;
        }

        Object model = node.getModelRef();
        if (model instanceof de.bund.zrb.model.TestCase) {
            currentTestCaseLog = new TestCaseLog(node.toString());
            currentSuiteLog.addChild(currentTestCaseLog);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            runNodeRecursive((TestNode) node.getChildAt(i));
        }

        if (model instanceof de.bund.zrb.model.TestCase) {
            currentTestCaseLog = null;
        }

        drawerRef.updateSuiteStatus(node);
    }


    private boolean playLeafAction(TestNode node) {
        TestAction action = node.getAction();
        if (action == null) {
            System.err.println("⚠️ Keine Action im Blattknoten gefunden!");
            return false;
        }

        // Logging vorbereiten
        StepLog stepLog = new StepLog(action.getType().name(), buildStepText(action));
        if (currentTestCaseLog != null) {
            currentTestCaseLog.addStep(stepLog);
        }

        return playSingleAction(action);
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
