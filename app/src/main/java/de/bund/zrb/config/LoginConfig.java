package de.bund.zrb.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Vereint:
 *  - Technische Login-Infos (Selektoren, Ziel-URLs)
 *  - Detection-Einstellungen (ehem. AuthDetectionConfig) – jetzt pro User
 *
 * Diese Klasse ist absichtlich POJO-artig gehalten (kein SettingsService-Load),
 * da sie in users.json mit dem jeweiligen User gespeichert/geladen wird.
 */
public final class LoginConfig {

    // --- Technische Login-Infos (UI-relevant) ---
    private String usernameSelector;
    private String passwordSelector;
    private String submitSelector;

    /** Login-Seite (vorher im User, jetzt thematisch hier) */
    private String loginPage;

    /** Optional: Seite für erzwungene Passwort-Änderung nach Login (Vorbereitung) */
    private String passwordChangePage;

    // Selektoren für Passwort-Änderungsseite
    private String currentPasswordSelector; // aktuelles Password (optional)
    private String newPasswordSelector;     // neues Password
    private String repeatPasswordSelector;  // neues Password wiederholen
    private String changeSubmitSelector;    // Submit-Button

    // --- Detection (ehem. AuthDetectionConfig) ---
    private Boolean enabled;
    private String sessionCookieName;
    private List<Integer> redirectStatusCodes;
    private List<String> loginUrlPrefixes;
    private List<String> passwordChangeUrlPrefixes;

    public LoginConfig() {
        // Defaults passend zu bisheriger Logik
        this.enabled = Boolean.TRUE;
        this.sessionCookieName = "JSESSIONID";
        this.redirectStatusCodes = new ArrayList<>(Arrays.asList(301, 302, 303, 307, 308));
        this.loginUrlPrefixes = new ArrayList<>(Arrays.asList("/login", "/signin"));
        this.passwordChangeUrlPrefixes = new ArrayList<>(Arrays.asList(
                "/password/change", "/password/expired", "/user/changePassword", "/change-password"
        ));
    }

    // ---------- Getters / Setters (technische Login-Infos) ----------
    public String getUsernameSelector() { return usernameSelector; }
    public void setUsernameSelector(String usernameSelector) { this.usernameSelector = usernameSelector; }

    public String getPasswordSelector() { return passwordSelector; }
    public void setPasswordSelector(String passwordSelector) { this.passwordSelector = passwordSelector; }

    public String getSubmitSelector() { return submitSelector; }
    public void setSubmitSelector(String submitSelector) { this.submitSelector = submitSelector; }

    public String getLoginPage() { return loginPage; }
    public void setLoginPage(String loginPage) { this.loginPage = loginPage; }

    public String getPasswordChangePage() { return passwordChangePage; }
    public void setPasswordChangePage(String passwordChangePage) { this.passwordChangePage = passwordChangePage; }

    public String getCurrentPasswordSelector() { return currentPasswordSelector; }
    public void setCurrentPasswordSelector(String s) { this.currentPasswordSelector = s; }
    public String getNewPasswordSelector() { return newPasswordSelector; }
    public void setNewPasswordSelector(String s) { this.newPasswordSelector = s; }
    public String getRepeatPasswordSelector() { return repeatPasswordSelector; }
    public void setRepeatPasswordSelector(String s) { this.repeatPasswordSelector = s; }
    public String getChangeSubmitSelector() { return changeSubmitSelector; }
    public void setChangeSubmitSelector(String s) { this.changeSubmitSelector = s; }

    // ---------- Getters / Setters (Detection) ----------
    public boolean isEnabled() { return enabled == null || enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getSessionCookieName() { return sessionCookieName; }
    public void setSessionCookieName(String sessionCookieName) { this.sessionCookieName = sessionCookieName; }

    public List<Integer> getRedirectStatusCodes() {
        return redirectStatusCodes == null ? Collections.emptyList() : Collections.unmodifiableList(redirectStatusCodes);
    }
    public void setRedirectStatusCodes(List<Integer> redirectStatusCodes) {
        this.redirectStatusCodes = (redirectStatusCodes == null)
                ? new ArrayList<>()
                : new ArrayList<>(redirectStatusCodes);
    }

    public List<String> getLoginUrlPrefixes() {
        return loginUrlPrefixes == null ? Collections.emptyList() : Collections.unmodifiableList(loginUrlPrefixes);
    }
    public void setLoginUrlPrefixes(List<String> loginUrlPrefixes) {
        this.loginUrlPrefixes = (loginUrlPrefixes == null)
                ? new ArrayList<>()
                : new ArrayList<>(loginUrlPrefixes);
    }

    public List<String> getPasswordChangeUrlPrefixes() {
        return passwordChangeUrlPrefixes == null ? Collections.emptyList() : Collections.unmodifiableList(passwordChangeUrlPrefixes);
    }
    public void setPasswordChangeUrlPrefixes(List<String> passwordChangeUrlPrefixes) {
        this.passwordChangeUrlPrefixes = (passwordChangeUrlPrefixes == null)
                ? new ArrayList<>()
                : new ArrayList<>(passwordChangeUrlPrefixes);
    }
}
