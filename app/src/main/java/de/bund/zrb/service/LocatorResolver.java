package de.bund.zrb.service;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.bund.zrb.util.LocatorType;
import de.bund.zrb.model.TestAction;

/**
 * Resolve Playwright locators based on LocatorType.
 * Keep selectors robust for JSF/PrimeFaces and support high-level locators.
 */
public final class LocatorResolver {

    private LocatorResolver() { /* Do not instantiate */ }

    public static Locator resolve(Page page, TestAction action) {
        if (page == null || action == null) {
            throw new IllegalArgumentException("page and action must not be null");
        }
        LocatorType type = action.getLocatorType();
        String selector = action.getSelectedSelector();
        if (type == null) type = LocatorType.CSS; // fallback
        if (selector == null) selector = "";

        switch (type) {
            case XPATH:
                return page.locator("xpath=" + selector);

            case CSS:
                // Assumed sanitized upstream; still pass-through as plain CSS
                return page.locator(selector);

            case ID:
                // Accept "#id" or raw "id"; always build attribute selector to survive colons
                if (selector.startsWith("#") || selector.startsWith("[id=")) {
                    return page.locator(selector);
                }
                return page.locator("[id='" + selector.replace("'", "\\'") + "']");

            case TEXT:
                return page.getByText(selector);

            case LABEL:
                return page.getByLabel(selector);

            case PLACEHOLDER:
                return page.getByPlaceholder(selector);

            case ALTTEXT:
                return page.getByAltText(selector);

            case ROLE:
                // Expect "role=<role>[;name=<name>]"
                String role = null;
                String name = null;
                String[] parts = selector.split(";");
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim();
                    int idx = p.indexOf('=');
                    if (idx <= 0) continue;
                    String k = p.substring(0, idx).trim();
                    String v = p.substring(idx + 1).trim();
                    if ("role".equalsIgnoreCase(k)) role = v;
                    if ("name".equalsIgnoreCase(k)) name = v;
                }
                if (role != null) {
                    try {
                        AriaRole ariaRole = AriaRole.valueOf(role.trim().toUpperCase().replace('-', '_'));
                        Page.GetByRoleOptions opts = new Page.GetByRoleOptions();
                        if (name != null && name.length() > 0) {
                            opts.setName(name);
                        }
                        return page.getByRole(ariaRole, opts);
                    } catch (IllegalArgumentException ex) {
                        // Fallback for unknown role tokens: CSS role selector; refine by text if available
                        Locator base = page.locator("[role='" + role + "']");
                        if (name != null && name.length() > 0) {
                            // Playwright Java supports hasText on filter()
                            return base.filter(new Locator.FilterOptions().setHasText(name));
                        }
                        return base;
                    }
                }
                // If role missing, fall back to text
                if (name != null && name.length() > 0) {
                    return page.getByText(name);
                }
                return page.locator("*"); // degenerate fallback

            default:
                return page.locator(selector);
        }
    }
}
