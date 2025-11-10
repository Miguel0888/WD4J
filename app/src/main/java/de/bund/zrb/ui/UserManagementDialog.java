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

    private final JTextField loginPageField;
    private final JTextField passwordChangePageField;

    private final JTextField otpField;
    private final DefaultComboBoxModel<UserRegistry.User> comboModel;

    public UserManagementDialog(Frame owner) {
        super(owner, "Benutzerverwaltung", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 400);
        setLocationRelativeTo(owner);

        comboModel = new DefaultComboBoxModel<>();
        userCombo = new JComboBox<>(comboModel);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        startPageField = new JTextField(20);
        loginPageField = new JTextField(20);
        passwordChangePageField = new JTextField(20);
        otpField = new JTextField(20);

        JButton saveButton = new JButton("Speichern");
        JButton newButton = new JButton("Neu");
        JButton deleteButton = new JButton("Löschen");

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

        // Passwort
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Passwort:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(passwordField, gbc);

        // Startseite
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        content.add(new JLabel("Startseite:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(startPageField, gbc);

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
        btns.add(newButton);
        btns.add(deleteButton);
        btns.add(saveButton);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        content.add(btns, gbc);

        setContentPane(content);

        // Laden
        refreshUserList();

        userCombo.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u != null) {
                usernameField.setText(u.getUsername());
                passwordField.setText(u.getDecryptedPassword());
                startPageField.setText(u.getStartPage());
                otpField.setText(u.getOtpSecret());

                LoginConfig lc = u.getLoginConfig();
                loginPageField.setText(lc.getLoginPage());
                passwordChangePageField.setText(lc.getPasswordChangePage());
            }
        });

        newButton.addActionListener(e -> {
            UserRegistry.User newUser = new UserRegistry.User(
                    "Neuer Benutzer", "", "", "", new LoginConfig()
            );
            UserRegistry.getInstance().addUser(newUser);
            refreshUserList();
            userCombo.setSelectedItem(newUser);
        });

        saveButton.addActionListener(e -> {
            UserRegistry.User u = (UserRegistry.User) userCombo.getSelectedItem();
            if (u == null) return;

            u.setUsername(usernameField.getText().trim());
            u.setEncryptedPassword(WindowsCryptoUtil.encrypt(new String(passwordField.getPassword())));
            u.setStartPage(startPageField.getText().trim());
            u.setOtpSecret(otpField.getText().trim());

            LoginConfig lc = u.getLoginConfig();
            lc.setLoginPage(loginPageField.getText().trim());
            lc.setPasswordChangePage(passwordChangePageField.getText().trim());

            UserRegistry.getInstance().save();
        });

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
    }

    private void refreshUserList() {
        comboModel.removeAllElements();
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        for (UserRegistry.User user : users) comboModel.addElement(user);
        if (comboModel.getSize() > 0) userCombo.setSelectedIndex(0);
    }
}
