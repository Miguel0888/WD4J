package de.bund.zrb.ui;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.components.OtpTestDialog;
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
    private final JTextField otpField;

    private final DefaultComboBoxModel<UserRegistry.User> comboModel;

    public UserManagementDialog(Frame owner) {
        super(owner, "Benutzerverwaltung", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(owner);

        comboModel = new DefaultComboBoxModel<>();
        userCombo = new JComboBox<>(comboModel);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        startPageField = new JTextField(20);
        loginPageField = new JTextField(20);
        otpField = new JTextField(20);

        JButton saveButton = new JButton("Speichern");
        JButton newButton = new JButton("Neu");
        JButton deleteButton = new JButton("Löschen");

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // User-Dropdown
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("Benutzer wählen:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(userCombo, gbc);

        // Username
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; // Fix label
        content.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(usernameField, gbc);

        // Passwort
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("Passwort:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(passwordField, gbc);

        // Startseite
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("Startseite:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(startPageField, gbc);

        // Loginseite
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("Login-Seite:"), gbc);

        // Panel mit Textfeld + Button
        JPanel loginPanel  = new JPanel(new BorderLayout(5, 0));
        loginPanel.add(loginPageField, BorderLayout.CENTER);

        // Button mit Unicode-Symbol und Tooltip
        JButton configButton = new JButton("🛠");
        configButton.setToolTipText("Login-Felder konfigurieren");
        configButton.setMargin(new Insets(2, 6, 2, 6));
        loginPanel.add(configButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(loginPanel , gbc);

        // 2FA
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("2FA (optional):"), gbc);

        // Panel mit Textfeld + Button
        JPanel otpPanel = new JPanel(new BorderLayout(5, 0));
        otpPanel.add(otpField, BorderLayout.CENTER);

        // Button mit Unicode-Symbol und Tooltip
        JButton testOtpButton = new JButton("⏱"); // Alternative: 🔍
        testOtpButton.setToolTipText("Aktuellen OTP-Code anzeigen");
        testOtpButton.setMargin(new Insets(2, 6, 2, 6)); // Kompakte Darstellung
        otpPanel.add(testOtpButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(otpPanel, gbc);

        // Buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(newButton, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(saveButton, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(deleteButton, gbc);

        setContentPane(content);


        // Laden
        refreshUserList();

        userCombo.addActionListener(e -> {
            UserRegistry.User selected = (UserRegistry.User) userCombo.getSelectedItem();
            if (selected != null) {
                usernameField.setText(selected.getUsername());
                passwordField.setText(selected.getDecryptedPassword());
                startPageField.setText(selected.getStartPage());
                loginPageField.setText(selected.getLoginPage());
                otpField.setText(selected.getOtpSecret());
            }
        });

        newButton.addActionListener(e -> {
            UserRegistry.User newUser = new UserRegistry.User("Neuer Benutzer", "", "", "", null);
            UserRegistry.getInstance().addUser(newUser);
            refreshUserList();
            userCombo.setSelectedItem(newUser);
        });

        saveButton.addActionListener(e -> {
            UserRegistry.User selected = (UserRegistry.User) userCombo.getSelectedItem();
            if (selected == null) return;

            selected.setUsername(usernameField.getText());
            selected.setEncryptedPassword(WindowsCryptoUtil.encrypt(new String(passwordField.getPassword())));
            selected.setStartPage(startPageField.getText());
            selected.setLoginPage(loginPageField.getText());
            selected.setOtpSecret(otpField.getText());

            UserRegistry.getInstance().save();
            JOptionPane.showMessageDialog(this, "Benutzer gespeichert!");
        });

        deleteButton.addActionListener(e -> {
            UserRegistry.User selected = (UserRegistry.User) userCombo.getSelectedItem();
            if (selected == null) return;

            UserRegistry.getInstance().removeUser(selected);
            refreshUserList();
        });

        testOtpButton.addActionListener(e -> {
            UserRegistry.User selected = (UserRegistry.User) userCombo.getSelectedItem();
            if (selected == null || selected.getOtpSecret() == null || selected.getOtpSecret().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kein OTP-Secret hinterlegt für diesen Benutzer.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            new OtpTestDialog(this, selected).setVisible(true);
        });

        configButton.addActionListener(e -> {
            UserRegistry.User user = (UserRegistry.User) userCombo.getSelectedItem();
            if (user == null) return;

            UserRegistry.User.LoginConfig config = user.getLoginConfig();

            JTextField usernameField = new JTextField(config.getUsernameSelector(), 25);
            JTextField passwordField = new JTextField(config.getPasswordSelector(), 25);
            JTextField submitField = new JTextField(config.getSubmitSelector(), 25);

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Username-Feld (Selector):"));
            panel.add(usernameField);
            panel.add(new JLabel("Passwort-Feld (Selector):"));
            panel.add(passwordField);
            panel.add(new JLabel("Login-Button (Selector):"));
            panel.add(submitField);

            int result = JOptionPane.showConfirmDialog(
                    this, panel, "Login-Konfiguration", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                config.setUsernameSelector(usernameField.getText().trim());
                config.setPasswordSelector(passwordField.getText().trim());
                config.setSubmitSelector(submitField.getText().trim());
            }
        });

    }

    private void refreshUserList() {
        comboModel.removeAllElements();
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        for (UserRegistry.User user : users) {
            comboModel.addElement(user);
        }
    }
}
