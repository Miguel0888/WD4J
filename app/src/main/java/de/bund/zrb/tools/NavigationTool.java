package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserRegistry;

public class NavigationTool extends AbstractUserTool {

    private final BrowserService browserService;

    public NavigationTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    /**
     * Navigiert die Seite des aktuellen Benutzers zur konfigurierten Startseite.
     */
    public void navigateToStartPage() {
        UserRegistry.User currentUser = getCurrentUserOrFail();
        navigateToStartPage(currentUser);
    }

    /**
     * Navigiert die Seite des angegebenen Benutzers zur konfigurierten Startseite.
     */
    public void navigateToStartPage(UserRegistry.User user) {
        if (user == null) {
            throw new IllegalArgumentException("Benutzer darf nicht null sein.");
        }

        String startPage = user.getStartPage();
        if (startPage == null || startPage.trim().isEmpty()) {
            throw new IllegalStateException("Startseite für Benutzer " + user.getUsername() + " ist nicht gesetzt.");
        }

        Page page = browserService.getActivePage(user.getUsername());
        if (page == null) {
            throw new IllegalStateException("Keine aktive Seite für Benutzer " + user.getUsername() + " vorhanden.");
        }

        System.out.println("🌐 Navigiere Benutzer '" + user.getUsername() + "' zu: " + startPage);
        page.navigate(startPage);
    }
}
