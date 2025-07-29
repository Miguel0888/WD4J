package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TestPlayerService {

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

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
        if (node == null) {
            node = drawerRef.getRootNode();
        }

        runNodeStepByStep(node);

        if (stopped) {
            logger.append(new SuiteLog("⏹ Playback abgebrochen!"));
        }
    }

    private void runNodeStepByStep(TestNode node) {
        if (stopped) return;
        Object model = node.getModelRef();

        if (model instanceof TestAction) {
            TestAction action = (TestAction) model;
            playSingleAction(action);
            logger.append(new StepLog(action.getType().name(), buildStepText(action)));
            drawerRef.updateNodeStatus(node, true); // ggf. Fehlerhandling ergänzen
            return;
        }

        logger.append(new SuiteLog(node.toString())); // sofort loggen

        for (int i = 0; i < node.getChildCount(); i++) {
            runNodeStepByStep((TestNode) node.getChildAt(i));
        }

        drawerRef.updateSuiteStatus(node);
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
