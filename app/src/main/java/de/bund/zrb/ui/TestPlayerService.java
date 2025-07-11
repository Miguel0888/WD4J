package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.LeftDrawer;

import java.util.List;

public class TestPlayerService {

    private static final TestPlayerService INSTANCE = new TestPlayerService();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private LeftDrawer drawerRef;

    private TestPlayerService() {}

    public static TestPlayerService getInstance() {
        return INSTANCE;
    }

    public void registerDrawer(LeftDrawer drawer) {
        this.drawerRef = drawer;
    }

    public List<TestSuite> getSuitesToRun() {
        if (drawerRef != null) {
            return drawerRef.getSelectedSuites();
        } else {
            System.err.println("⚠️ Kein LeftDrawer registriert!");
            return null;
        }
    }

    public void runSuites(List<TestSuite> suites) {
        for (TestSuite suite : suites) {
            runSuite(suite);
        }
    }

    public void runSuite(TestSuite suite) {
        for (TestCase testCase : suite.getTestCases()) {
            runTestCase(testCase);
        }
    }

    private void runTestCase(TestCase testCase) {
        for (TestAction action : testCase.getWhen()) {
            playAction(action);
        }
    }

    private void playAction(TestAction action) {
        String username = action.getUser();
        if (username == null || username.isEmpty()) {
            System.err.println("⚠️ Keine User-Zuordnung für Action: " + action.getAction());
            return;
        }

        Page page = browserService.getActivePage(username);

        switch (action.getAction()) {
            case "navigate":
                page.navigate(action.getValue());
                break;
            case "click":
                page.locator(action.getSelectedSelector()).click();
                break;
            case "input":
            case "fill":
                Locator locator = page.locator(action.getSelectedSelector());
                locator.fill(action.getValue());
                break;
            case "wait":
                try {
                    long waitTime = Long.parseLong(action.getValue());
                    Thread.sleep(waitTime);
                } catch (Exception ignored) {}
                break;
            default:
                System.out.println("⚠️ Nicht unterstützte Action: " + action.getAction());
        }
    }

}
