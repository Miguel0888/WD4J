package de.bund.zrb.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.BrowserServiceImpl;

public class TestSuitePlayer {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

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

        // Hole Page sicher über den Service
        Page page = browserService.getActivePage(username);

        switch (action.getAction()) {
            case "navigate":
                page.navigate(action.getValue());
                break;
            case "click":
                page.locator(action.getSelectedSelector()).click();
                break;
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
