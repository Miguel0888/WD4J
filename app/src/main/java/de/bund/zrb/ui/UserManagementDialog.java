package de.bund.zrb.ui;

import de.bund.zrb.config.LoginConfig;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.debug.OtpTestDialog;
import de.bund.zrb.util.WindowsCryptoUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class UserManagementDialog extends JDialog {

    private final JComboBox<UserRegistry.User> userCombo;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField startPageField;
    private final JCheckBox autoOpenStartPageOnLaunchCheck;

    private final JTextField loginPageField;
    private final JTextField passwordChangePageField;

    private final JTextField otpField;
    private final DefaultComboBoxModel<UserRegistry.User> comboModel;

    // Passwort-UI State
    private boolean passwordVisible = false;
    private char passwordEchoChar;

    public UserManagementDialog(Frame owner) {
        super(owner, "Benutzerverwaltung", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 400);
        setLocationRelativeTo(owner);

        comboModel = new DefaultComboBoxModel<>();
        userCombo = new JComboBox<>(comboModel);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        // Merke das Standard-Echo-Char, damit wir es beim Verbergen wiederherstellen können
        passwordEchoChar = passwordField.getEchoChar();

        startPageField = new JTextField(20);
        autoOpenStartPageOnLaunchCheck = new JCheckBox();
        // Icon-Checkbox Styling (Haus-Symbol im Kreis)
        autoOpenStartPageOnLaunchCheck.setToolTipText("Beim Browserstart automatisch neuen Tab mit der Startseite öffnen");
        autoOpenStartPageOnLaunchCheck.setFocusPainted(false);
        autoOpenStartPageOnLaunchCheck.setBorderPainted(false);
        autoOpenStartPageOnLaunchCheck.setContentAreaFilled(false);
        autoOpenStartPageOnLaunchCheck.setOpaque(false);
        autoOpenStartPageOnLaunchCheck.setPreferredSize(new Dimension(28,28));
        autoOpenStartPageOnLaunchCheck.getAccessibleContext().setAccessibleName("Auto-Startseite aktivieren");
        // Initial Icons setzen
        autoOpenStartPageOnLaunchCheck.setIcon(createCircleIcon(new Color(170,170,170), "🏠"));
        autoOpenStartPageOnLaunchCheck.setSelectedIcon(createCircleIcon(new Color(0,150,0), "🏠"));
        autoOpenStartPageOnLaunchCheck.addItemListener(e -> {
            // Bei Änderung neu zeichnen (für High-DPI klare Darstellung)
            if (autoOpenStartPageOnLaunchCheck.isSelected()) {
                autoOpenStartPageOnLaunchCheck.setIcon(createCircleIcon(new Color(0,150,0), "🏠"));
            } else {
                autoOpenStartPageOnLaunchCheck.setIcon(createCircleIcon(new Color(170,170,170), "🏠"));
            }
        });

        loginPageField = new JTextField(20);
        passwordChangePageField = new JTextField(20);
        otpField = new JTextField(20);

        JButton saveButton = new JButton("Speichern");
        JButton newButton = new JButton("Neu");
        JButton deleteButton = new JButton("Löschen");
        JButton okButton = new JButton("OK");

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Benutzer
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("Benutzer wählen:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(userCombo, gbc);

        // Username
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(usernameField, gbc);

        // Passwort (mit Einblenden-Button)
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Passwort:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        // Panel mit Passwortfeld und Button ähnlich zu anderen Konfig-Buttons
        JPanel pwPanel = new JPanel(new BorderLayout(5, 0));
        pwPanel.add(passwordField, BorderLayout.CENTER);
        JButton showPasswordBtn = new JButton("👁");
        showPasswordBtn.setToolTipText("Passwort einblenden");
        showPasswordBtn.setMargin(new Insets(2, 6, 2, 6));
        pwPanel.add(showPasswordBtn, BorderLayout.EAST);
        content.add(pwPanel, gbc);

        // Listener für Ein-/Ausblenden
        showPasswordBtn.addActionListener(e -> {
            if (!passwordVisible) {
                // sichtbar: Echo entfernen
                passwordField.setEchoChar((char) 0);
                showPasswordBtn.setText("🙈");
                showPasswordBtn.setToolTipText("Passwort verbergen");
                passwordVisible = true;
            } else {
                // verbergen: ursprüngliches Echo wiederherstellen
                passwordField.setEchoChar(passwordEchoChar);
                showPasswordBtn.setText("👁");
                showPasswordBtn.setToolTipText("Passwort einblenden");
                passwordVisible = false;
            }
        });

        // Startseite
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Startseite:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel startPagePanel = new JPanel(new BorderLayout(5,0));
        startPagePanel.add(startPageField, BorderLayout.CENTER);
        autoOpenStartPageOnLaunchCheck.setText("");
        autoOpenStartPageOnLaunchCheck.setToolTipText("Beim Browserstart automatisch neuen Tab mit der Startseite öffnen");
        startPagePanel.add(autoOpenStartPageOnLaunchCheck, BorderLayout.EAST);
        content.add(startPagePanel, gbc);

        // Login-Seite (aus LoginConfig)
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Login-Seite:"), gbc);
        JPanel loginPanel  = new JPanel(new BorderLayout(5, 0));
        loginPanel.add(loginPageField, BorderLayout.CENTER);
        JButton configButton = new JButton("🛠");
        configButton.setToolTipText("Login-Felder konfigurieren");
        configButton.setMargin(new Insets(2, 6, 2, 6));
        loginPanel.add(configButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(loginPanel , gbc);

        // Passwort-Änderungs-Seite (optional)
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Passwort-Änderungs-Seite (optional):"), gbc);
        JPanel pwChangePanel = new JPanel(new BorderLayout(5, 0));
        pwChangePanel.add(passwordChangePageField, BorderLayout.CENTER);
        JButton pwChangeConfigBtn = new JButton("🛠");
        pwChangeConfigBtn.setToolTipText("Selectors für Passwort-Änderung konfigurieren");
        pwChangeConfigBtn.setMargin(new Insets(2, 6, 2, 6));
        pwChangePanel.add(pwChangeConfigBtn, BorderLayout.EAST);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(pwChangePanel, gbc);

        // 2FA
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("2FA (optional):"), gbc);
        JPanel otpPanel = new JPanel(new BorderLayout(5, 0));
        otpPanel.add(otpField, BorderLayout.CENTER);
        JButton testOtpButton = new JButton("⏱");
        testOtpButton.setToolTipText("Aktuellen OTP-Code anzeigen");
        testOtpButton.setMargin(new Insets(2, 6, 2, 6));
        otpPanel.add(testOtpButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(otpPanel, gbc);

        // Buttons
        row++;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        // OK ganz links: zuerst hinzufügen, aber FlowLayout RIGHT richtet rechts aus – daher eigene Reihenfolge
        btns.add(okButton);
        btns.add(newButton);
        btns.add(deleteButton);
        btns.add(saveButton);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        content.add(btns, gbc);

        setContentPane(content);

        // Listener zuerst registrieren (wichtig: bevor die ComboModel befüllt wird)
        userCombo.addActionListener(e -> loadSelectedUser());

        newButton.addActionListener(e -> {
            UserRegistry.User newUser = new UserRegistry.User(
                    "Neuer Benutzer", "", "", "", new LoginConfig()
            );
            newUser.setAutoOpenStartPageOnLaunch(true); // Default aktiv
            UserRegistry.getInstance().addUser(newUser);
            refreshUserList();
            userCombo.setSelectedItem(newUser);
        });

        // Gemeinsame Speicher-Logik extrahiert
        Runnable saveSelectedUser = () -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null) return;
            u.setUsername(usernameField.getText().trim());
            u.setEncryptedPassword(WindowsCryptoUtil.encrypt(new String(passwordField.getPassword())));
            u.setStartPage(startPageField.getText().trim());
            u.setAutoOpenStartPageOnLaunch(autoOpenStartPageOnLaunchCheck.isSelected());
            u.setOtpSecret(otpField.getText().trim());
            LoginConfig lc = u.getLoginConfig();
            lc.setLoginPage(loginPageField.getText().trim());
            lc.setPasswordChangePage(passwordChangePageField.getText().trim());
            UserRegistry.getInstance().save();
        };

        saveButton.addActionListener(e -> saveSelectedUser.run());
        okButton.addActionListener(e -> { saveSelectedUser.run(); dispose(); });

        deleteButton.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null) return;
            UserRegistry.getInstance().removeUser(u);
            refreshUserList();
        });

        // 🛠 Selektoren konfigurieren (Login)
        configButton.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null) return;
            LoginConfig config = u.getLoginConfig();

            JTextField usernameSel = new JTextField(config.getUsernameSelector(), 25);
            JTextField passwordSel = new JTextField(config.getPasswordSelector(), 25);
            JTextField submitSel   = new JTextField(config.getSubmitSelector(), 25);

            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel("Username-Feld (Selector):"));
            panel.add(usernameSel);
            panel.add(new JLabel("Passwort-Feld (Selector):"));
            panel.add(passwordSel);
            panel.add(new JLabel("Login-Button (Selector):"));
            panel.add(submitSel);

            int result = JOptionPane.showConfirmDialog(this, panel, "Login-Konfiguration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                config.setUsernameSelector(usernameSel.getText().trim());
                config.setPasswordSelector(passwordSel.getText().trim());
                config.setSubmitSelector(submitSel.getText().trim());
            }
        });

        // 🛠 Selektoren konfigurieren (Passwort-Änderung)
        pwChangeConfigBtn.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null) return;
            LoginConfig config = u.getLoginConfig();

            JTextField currentPwSel = new JTextField(config.getCurrentPasswordSelector(), 25);
            JTextField newPwSel     = new JTextField(config.getNewPasswordSelector(), 25);
            JTextField repeatPwSel  = new JTextField(config.getRepeatPasswordSelector(), 25);
            JTextField submitSel    = new JTextField(config.getChangeSubmitSelector(), 25);

            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel("Aktuelles Passwort (Selector, optional):"));
            panel.add(currentPwSel);
            panel.add(new JLabel("Neues Passwort (Selector):"));
            panel.add(newPwSel);
            panel.add(new JLabel("Neues Passwort wiederholen (Selector):"));
            panel.add(repeatPwSel);
            panel.add(new JLabel("Änderung absenden (Selector):"));
            panel.add(submitSel);

            int result = JOptionPane.showConfirmDialog(this, panel, "Passwort-Änderung – Konfiguration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                config.setCurrentPasswordSelector(currentPwSel.getText().trim());
                config.setNewPasswordSelector(newPwSel.getText().trim());
                config.setRepeatPasswordSelector(repeatPwSel.getText().trim());
                config.setChangeSubmitSelector(submitSel.getText().trim());
            }
        });

        testOtpButton.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null || u.getOtpSecret() == null || u.getOtpSecret().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kein OTP-Secret hinterlegt für diesen Benutzer.",
                        "Hinweis", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            new OtpTestDialog(this, u).setVisible(true);
        });

        // Liste befüllen und initiale Auswahl laden
        refreshUserList();
        loadSelectedUser();
    }

    private void loadSelectedUser() {
        UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
        if (u == null) {
            usernameField.setText("");
            passwordField.setText("");
            startPageField.setText("");
            autoOpenStartPageOnLaunchCheck.setSelected(false);
            otpField.setText("");
            loginPageField.setText("");
            passwordChangePageField.setText("");
            return;
        }
        usernameField.setText(u.getUsername());
        passwordField.setText(u.getDecryptedPassword());
        startPageField.setText(u.getStartPage());
        autoOpenStartPageOnLaunchCheck.setSelected(u.isAutoOpenStartPageOnLaunch());
        otpField.setText(u.getOtpSecret());

        LoginConfig lc = u.getLoginConfig();
        if (lc != null) {
            loginPageField.setText(lc.getLoginPage());
            passwordChangePageField.setText(lc.getPasswordChangePage());
        } else {
            loginPageField.setText("");
            passwordChangePageField.setText("");
        }
    }

    private void refreshUserList() {
        comboModel.removeAllElements();
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        if (users != null) {
            for (UserRegistry.User user : users) comboModel.addElement(user);
        }
        if (comboModel.getSize() > 0) userCombo.setSelectedIndex(0);
    }

    // Hilfsfunktion zum Erzeugen eines runden Icon-Buttons mit Unicode-Glyph
    private static ImageIcon createCircleIcon(Color fill, String glyph) {
        int size = 24; // Durchmesser
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Hintergrund transparent, Kreis zeichnen
        g.setColor(fill);
        g.fillOval(0,0,size-1,size-1);
        // Glyph in Weiß zentriert
        g.setColor(Color.WHITE);
        Font font = new Font("SansSerif", Font.PLAIN, 13);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(glyph);
        int textH = fm.getAscent();
        int x = (size - textW)/2;
        int y = (size + textH)/2 - 3;
        g.drawString(glyph, x, y);
        g.dispose();
        return new ImageIcon(img);
    }
}
