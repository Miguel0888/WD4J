package de.bund.zrb.impl.playwright;

import com.microsoft.Selectors;

import java.nio.file.Path;

/**
 *  NOT SUPPORTED YET
 */
public class SelectorsImpl implements Selectors {

    /**
     * Selectors must be registered before creating the page.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> An example of registering selector engine that queries elements based on a tag name:
     * <pre>{@code
     * // Script that evaluates to a selector engine instance. The script is evaluated in the page context.
     * String createTagNameEngine = "{\n" +
     *   "  // Returns the first element matching given selector in the root's subtree.\n" +
     *   "  query(root, selector) {\n" +
     *   "    return root.querySelector(selector);\n" +
     *   "  },\n" +
     *   "  // Returns all elements matching given selector in the root's subtree.\n" +
     *   "  queryAll(root, selector) {\n" +
     *   "    return Array.from(root.querySelectorAll(selector));\n" +
     *   "  }\n" +
     *   "}";
     * // Register the engine. Selectors will be prefixed with "tag=".
     * playwright.selectors().register("tag", createTagNameEngine);
     * Browser browser = playwright.firefox().launch();
     * Page page = browser.newPage();
     * page.setContent("<div><button>Click me</button></div>");
     * // Use the selector prefixed with its name.
     * Locator button = page.locator("tag=button");
     * // Combine it with built-in locators.
     * page.locator("tag=div").getByText("Click me").click();
     * // Can use it in any methods supporting selectors.
     * int buttonCount = (int) page.locator("tag=button").count();
     * browser.close();
     * }</pre>
     *
     * @param name    Name that is used in selectors as a prefix, e.g. {@code {name: 'foo'}} enables {@code foo=myselectorbody} selectors. May
     *                only contain {@code [a-zA-Z0-9_]} characters.
     * @param script  Script that evaluates to a selector engine instance. The script is evaluated in the page context.
     * @param options
     * @since v1.8
     */
    @Override
    public void register(String name, String script, RegisterOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Selectors must be registered before creating the page.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> An example of registering selector engine that queries elements based on a tag name:
     * <pre>{@code
     * // Script that evaluates to a selector engine instance. The script is evaluated in the page context.
     * String createTagNameEngine = "{\n" +
     *   "  // Returns the first element matching given selector in the root's subtree.\n" +
     *   "  query(root, selector) {\n" +
     *   "    return root.querySelector(selector);\n" +
     *   "  },\n" +
     *   "  // Returns all elements matching given selector in the root's subtree.\n" +
     *   "  queryAll(root, selector) {\n" +
     *   "    return Array.from(root.querySelectorAll(selector));\n" +
     *   "  }\n" +
     *   "}";
     * // Register the engine. Selectors will be prefixed with "tag=".
     * playwright.selectors().register("tag", createTagNameEngine);
     * Browser browser = playwright.firefox().launch();
     * Page page = browser.newPage();
     * page.setContent("<div><button>Click me</button></div>");
     * // Use the selector prefixed with its name.
     * Locator button = page.locator("tag=button");
     * // Combine it with built-in locators.
     * page.locator("tag=div").getByText("Click me").click();
     * // Can use it in any methods supporting selectors.
     * int buttonCount = (int) page.locator("tag=button").count();
     * browser.close();
     * }</pre>
     *
     * @param name    Name that is used in selectors as a prefix, e.g. {@code {name: 'foo'}} enables {@code foo=myselectorbody} selectors. May
     *                only contain {@code [a-zA-Z0-9_]} characters.
     * @param script  Script that evaluates to a selector engine instance. The script is evaluated in the page context.
     * @param options
     * @since v1.8
     */
    @Override
    public void register(String name, Path script, RegisterOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Defines custom attribute name to be used in {@link PageImpl#getByTestId Page.getByTestId()}. {@code
     * data-testid} is used by default.
     *
     * @param attributeName Test id attribute name.
     * @since v1.27
     */
    @Override
    public void setTestIdAttribute(String attributeName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
