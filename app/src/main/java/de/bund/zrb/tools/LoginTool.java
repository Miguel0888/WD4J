package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.config.LoginConfig;
import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.util.WindowsCryptoUtil;

import javax.swing.*; // für einfachen Input-Dialog
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginTool extends AbstractUserTool implements BuiltinTool {

    private final BrowserService browserService;
    private final TotpService totpService;

    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.<>*%_?=#$-+";
    private static final int PASSWORD_LENGTH = 8;

    public LoginTool(BrowserService browserService, TotpService totpService) {
        this.browserService = browserService;
        this.totpService = totpService;
    }

    public void loginCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail();
        login(user);
    }

    public void login(UserRegistry.User user){
        login(user, null);
    }

    public void login(UserRegistry.User user, Page page) {
        String loginUrl = user.getLoginConfig().getLoginPage();
        if (loginUrl == null || loginUrl.trim().isEmpty()) {
            throw new IllegalStateException("Login-Seite nicht definiert für Benutzer: " + user.getUsername());
        }

        if( page == null)
        { // if no page is determined use the current selected one
            page = browserService.getActivePage(user.getUsername());
            page.navigate(loginUrl);
        }

        LoginConfig config = user.getLoginConfig();

        if (config.getUsernameSelector() == null ||
                config.getPasswordSelector() == null ||
                config.getSubmitSelector() == null) {
            throw new IllegalStateException("Login-Konfiguration unvollständig für Benutzer: " + user.getUsername());
        }

        System.out.println("🔐 Führe Login durch für " + user.getUsername());

        page.locator(config.getUsernameSelector()).waitFor();
        page.fill(config.getUsernameSelector(), user.getUsername());
        page.fill(config.getPasswordSelector(), user.getDecryptedPassword());
        page.click(config.getSubmitSelector());

        // Optional: OTP hinterher
        if (user.getOtpSecret() != null && !user.getOtpSecret().isEmpty()) {
            String otp = String.format("%06d", totpService.generateCurrentOtp(user.getOtpSecret()));
            System.out.println("🔢 OTP-Code: " + otp);
            // → ggf. weitere Verarbeitung, je nach Zielseite
        }
    }

    /**
     * Führt einen Passwort-Änderungsflow aus: Seite aufrufen (falls konfiguriert), Felder füllen und absenden.
     * Neues Passwort wird automatisch generiert (genau 8 Zeichen) und anschließend im User gespeichert.
     */
    public void changePasswordForCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail();
        changePassword(user, null);
    }

    public void changePassword(UserRegistry.User user, Page page) {
        LoginConfig cfg = user.getLoginConfig();
        if (cfg == null) {
            throw new IllegalStateException("LoginConfig fehlt für Benutzer: " + user.getUsername());
        }

        // Seite öffnen, falls vorhanden
        String pwChangeUrl = cfg.getPasswordChangePage();
        if (page == null) {
            page = browserService.getActivePage(user.getUsername());
            if (pwChangeUrl != null && !pwChangeUrl.trim().isEmpty()) {
                page.navigate(pwChangeUrl);
            }
        }

        String curSel   = cfg.getCurrentPasswordSelector();
        String newSel   = cfg.getNewPasswordSelector();
        String repSel   = cfg.getRepeatPasswordSelector();
        String submitSel= cfg.getChangeSubmitSelector();

        if (newSel == null || repSel == null || submitSel == null) {
            throw new IllegalStateException("Passwort-Änderungs-Konfiguration unvollständig (neues/Repeat/Submit) für Benutzer: " + user.getUsername());
        }

        System.out.println("🔏 Passwort-Änderung für " + user.getUsername());

        // Optional: aktuelles Passwort füllen, wenn Feld existiert
        if (curSel != null && !curSel.trim().isEmpty()) {
            page.locator(curSel).waitFor();
            page.fill(curSel, user.getDecryptedPassword());
        }

        String oldPassword = user.getDecryptedPassword();
        String suggested = generateCompliantPassword8(user, oldPassword); // Vorschlag erzeugen

        // Nutzer nach Passwort fragen (GUI-Dialog) – Vorschlag vorausgefüllt
        String entered = null;
        try {
            entered = (String) JOptionPane.showInputDialog(null,
                    "Neues Passwort (8 Zeichen) eingeben oder Vorschlag übernehmen:",
                    "Passwort ändern",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    suggested);
        } catch (Exception e) {
            // Falls UI im Headless-Modus nicht verfügbar ist – wir nutzen den Vorschlag
            System.out.println("ℹ️ Eingabedialog nicht verfügbar, verwende vorgeschlagenes Passwort.");
        }

        String newPassword = suggested; // Default
        if (entered != null) {
            entered = entered.trim();
            if (!entered.isEmpty()) {
                if (isManualPasswordCompliant(entered, user.getUsername(), oldPassword)) {
                    newPassword = entered;
                } else {
                    try {
                        JOptionPane.showMessageDialog(null,
                                "Eingegebenes Passwort erfüllt nicht alle Regeln – Vorschlag wird verwendet.",
                                "Ungültiges Passwort",
                                JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ignored) {
                        // Headless: einfach fortfahren
                    }
                }
            }
        }

        // Felder füllen
        page.locator(newSel).waitFor();
        page.fill(newSel, newPassword);
        page.locator(repSel).waitFor();
        page.fill(repSel, newPassword);

        // Absenden
        page.click(submitSel);

        // Vor dem Speichern: Nutzer explizit bestätigen lassen (Button 3 Sekunden deaktiviert)
        boolean shouldPersist = false;
        try {
            shouldPersist = confirmSaveWithDelay();
        } catch (Exception e) {
            System.out.println("ℹ️ Bestätigungsdialog nicht verfügbar, speichere NICHT automatisch.");
        }

        // Benutzer mit neuem Passwort aktualisieren (verschlüsselt) und persistieren
        if (shouldPersist) {
            try {
                user.setEncryptedPassword(WindowsCryptoUtil.encrypt(newPassword));
                UserRegistry.getInstance().save();
                System.out.println("✅ Neues Passwort gesetzt und gespeichert (8 Zeichen).");
            } catch (Exception e) {
                System.out.println("⚠️ Neues Passwort konnte nicht gespeichert werden: " + e.getMessage());
            }
        } else {
            System.out.println("↩️ Passwort wurde nicht persistent gespeichert (Benutzer hat nicht bestätigt).");
        }
    }

    private static boolean isManualPasswordCompliant(String candidate, String username, String oldPassword) {
        if (candidate == null || candidate.length() != PASSWORD_LENGTH) return false;
        // Zeichen prüfen
        for (char c : candidate.toCharArray()) {
            if (ALLOWED_CHARS.indexOf(c) < 0) return false;
        }
        // Monat/Jahr (zweistellig) verbieten
        LocalDate now = LocalDate.now();
        String month2 = String.format("%02d", now.getMonthValue());
        String year2 = String.format("%02d", now.getYear() % 100);
        if (candidate.contains(month2) || candidate.contains(year2)) return false;

        // Max 3 Zeichen aus Username
        int userCharCount = 0;
        if (username != null) {
            for (char c : candidate.toCharArray()) {
                if (username.indexOf(c) >= 0) {
                    userCharCount++;
                    if (userCharCount > 3) return false;
                }
            }
        }
        // Sequenzen
        if (hasNumericAscendingOrDescendingRun(candidate, 3)) return false;
        // Ab Index 3 keine Übereinstimmung mit altem Passwort
        if (oldPassword != null) {
            for (int i = 3; i < PASSWORD_LENGTH && i < oldPassword.length(); i++) {
                if (candidate.charAt(i) == oldPassword.charAt(i)) return false;
            }
        }
        return true;
    }

    private static String generateCompliantPassword8(UserRegistry.User user, String oldPassword) {
        final String allowed = ALLOWED_CHARS;
        final int LENGTH = PASSWORD_LENGTH;
        SecureRandom rnd = new SecureRandom();
        String username = user.getUsername() == null ? "" : user.getUsername();
        String old = oldPassword == null ? "" : oldPassword;
        LocalDate now = LocalDate.now();
        String month2 = String.format("%02d", now.getMonthValue());
        String year2 = String.format("%02d", now.getYear() % 100);
        for (int attempt = 0; attempt < 1000; attempt++) {
            StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(allowed.charAt(rnd.nextInt(allowed.length())));
            }
            String candidate = sb.toString();
            if (candidate.contains(month2) || candidate.contains(year2)) continue;
            int userCharCount = 0;
            for (char c : candidate.toCharArray()) {
                if (username.indexOf(c) >= 0) {
                    userCharCount++;
                    if (userCharCount > 3) break;
                }
            }
            if (userCharCount > 3) continue;
            if (hasNumericAscendingOrDescendingRun(candidate, 3)) continue;
            boolean invalidOldMatch = false;
            for (int i = 3; i < LENGTH; i++) {
                if (i < old.length() && candidate.charAt(i) == old.charAt(i)) { invalidOldMatch = true; break; }
            }
            if (invalidOldMatch) continue;

            return candidate;
        }
        throw new IllegalStateException("Konnte nach 1000 Versuchen kein gültiges Passwort erzeugen");
    }

    private static boolean hasNumericAscendingOrDescendingRun(String s, int maxAllowedRunLength) {
        // true -> es existiert eine auf/absteigende Ziffern-Sequenz, die länger als maxAllowedRunLength ist
        if (s == null || s.length() <= 1) return false;
        int ascRun = 1;
        int descRun = 1;
        for (int i = 1; i < s.length(); i++) {
            char prev = s.charAt(i - 1);
            char cur = s.charAt(i);
            boolean bothDigits = Character.isDigit(prev) && Character.isDigit(cur);
            if (!bothDigits) {
                ascRun = 1;
                descRun = 1;
                continue;
            }
            int dp = prev - '0';
            int dc = cur - '0';
            if (dc == dp + 1) {
                ascRun++;
                descRun = 1;
            } else if (dc == dp - 1) {
                descRun++;
                ascRun = 1;
            } else {
                ascRun = 1;
                descRun = 1;
            }
            if (ascRun > maxAllowedRunLength || descRun > maxAllowedRunLength) return true;
        }
        return false;
    }

    private static boolean confirmSaveWithDelay() throws Exception {
        // Führt einen modalen Dialog aus, bei dem der "Speichern"-Button erst nach 3 Sekunden aktiv wird.
        final AtomicBoolean result = new AtomicBoolean(false);
        Runnable uiTask = () -> {
            final JDialog dialog = new JDialog((java.awt.Frame) null, "Passwort speichern?", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JLabel msg = new JLabel("<html>Die Passwortänderung wurde abgesendet.<br/>Soll das neue Passwort gespeichert werden?</html>");
            JButton btnSave = new JButton("Speichern");
            JButton btnCancel = new JButton("Nicht speichern");
            btnSave.setEnabled(false);

            btnSave.addActionListener(e -> { result.set(true); dialog.dispose(); });
            btnCancel.addActionListener(e -> { result.set(false); dialog.dispose(); });

            JPanel btnPanel = new JPanel();
            btnPanel.add(btnCancel);
            btnPanel.add(btnSave);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            msg.setAlignmentX(0.5f);
            btnPanel.setAlignmentX(0.5f);
            root.add(Box.createVerticalStrut(10));
            root.add(msg);
            root.add(Box.createVerticalStrut(10));
            root.add(btnPanel);
            root.add(Box.createVerticalStrut(10));

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setLocationRelativeTo(null);

            // Timer: 3 Sekunden später aktivieren
            Timer t = new Timer(3000, ev -> btnSave.setEnabled(true));
            t.setRepeats(false);
            t.start();

            dialog.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            uiTask.run();
        } else {
            SwingUtilities.invokeAndWait(uiTask);
        }
        return result.get();
    }

    public String login(String user, String pass) {
        login(new UserRegistry.User(user, pass));
        return "login";
    }
    public String login(String user, String pass, String pageId) {
        login(new UserRegistry.User(user, pass), browserService.pageForBrowsingContextId(pageId));
        return "login";
    }

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Login",
                        "Perform a login using username/password; optionally add a TOTP secret.",
                        ToolExpressionFunction.params("username", "password", "totpSecret?"),
                        Arrays.asList(
                                "Account user name.",
                                "Account password, encrypted.",
                                "Optional Page by its ID")
                ),
                2, 3,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String u = args.get(0);
                        String p = args.get(1);
                        String c = args.size() >= 3 ? args.get(2) : null;
                        return c != null && c.length() > 0 ? login(u, p, c) : login(u, p);
                    }
                }
        ));

        return list;
    }
}
