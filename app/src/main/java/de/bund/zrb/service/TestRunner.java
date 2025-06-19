package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Paths;

public class TestRunner {
    private final Page page;

    public TestRunner(Page page) {
        this.page = page;
    }

    public void runTest(TestCase testCase) {
        for (TestAction action : testCase.getGiven()) {
            executeAction(action);
        }
        for (TestAction action : testCase.getWhen()) {
            executeAction(action);
        }
        for (TestAction action : testCase.getThen()) {
            executeAction(action);
        }
    }

    private void executeAction(TestAction action) {
        switch (action.getAction()) {
            case "click":
                getLocator(action).click();
                break;
            case "type":
                getLocator(action).fill(action.getValue());
                break;
            case "screenshot":
                String image = "screenshot.png"; // TODO
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(image)));
                break;
        }
    }

    private Locator getLocator(TestAction action) {
        switch (action.getLocatorType()) {
            case "css":
                return page.locator(action.getSelectedSelector());
            case "xpath":
                return page.locator("xpath=" + action.getSelectedSelector());
            case "id":
                return page.locator("#" + action.getSelectedSelector());
            case "text":
                return page.getByText(action.getText());
            case "role":
                return page.getByRole(action.getRole());
            default:
                throw new IllegalArgumentException("Unknown locator type: " + action.getLocatorType());
        }
    }
}
