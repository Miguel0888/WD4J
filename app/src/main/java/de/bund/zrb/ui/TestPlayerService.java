package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.TestPlayerUi;

import javax.swing.*;
import java.util.List;

public class TestPlayerService {

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private TestPlayerUi drawerRef;

    private TestPlayerService() {}

    public static TestPlayerService getInstance() {
        return INSTANCE;
    }

    public void registerDrawer(TestPlayerUi playerUi) {
        this.drawerRef = playerUi;
    }

    public void runSuites() {
        if (drawerRef == null) {
            System.err.println("⚠️ Kein LeftDrawer registriert!");
            return;
        }

        TestNode node = drawerRef.getSelectedNode();
        if (node == null) {
            node = drawerRef.getRootNode(); // ✅ Wenn nix markiert ist → Root
        }

        runNodeRecursive(node);
    }

    private void runNodeRecursive(TestNode node) {
        if (node.getChildCount() == 0) {
            boolean passed = playLeafAction(node);
            drawerRef.updateNodeStatus(node, passed);
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                runNodeRecursive((TestNode) node.getChildAt(i));
            }
            drawerRef.updateSuiteStatus(node);
        }
    }

    private boolean playLeafAction(TestNode node) {
        TestAction action = node.getAction(); // ✅ jetzt sauber direkt im Node!
        if (action == null) {
            System.err.println("⚠️ Keine Action im Blattknoten gefunden!");
            return false;
        }
        return playSingleAction(action);
    }

    public boolean playSingleAction(TestAction action) {
        try {
            String username = action.getUser();
            if (username == null || username.isEmpty()) {
                System.err.println("⚠️ Keine User-Zuordnung für Action: " + action.getAction());
                return false;
            }

            Page page = browserService.getActivePage(username);
//
//            // ToDo: Später an PlayWrigh-API weiterreichen, damit immer nur so lange wie nötig gewartet wird:
//            long timeout = action.getTimeout();
//            if (timeout > 0) {
//                System.out.println("⏳ Warte global " + timeout + " ms vor Action...");
//                Thread.sleep(timeout);
//            }

            switch (action.getAction()) {
                case "navigate":
                    page.navigate(action.getValue(), new Page.NavigateOptions().setTimeout(action.getTimeout()));
                    break;
                case "click":
                    Locator clickLocator = page.locator(action.getSelectedSelector());
//                    clickLocator.waitFor(new Locator.WaitForOptions().setTimeout(action.getTimeout()));
                    Thread.sleep(action.getTimeout());
                    clickLocator.click();
                    break;
                case "input":
                case "fill":
                    Locator fillLocator = page.locator(action.getSelectedSelector());
                    fillLocator.waitFor(new Locator.WaitForOptions().setTimeout(action.getTimeout()));
                    fillLocator.fill(action.getValue());
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
