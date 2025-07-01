package de.bund.zrb.ui;

import de.bund.zrb.service.UserRegistry;
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

        // 2FA
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        content.add(new JLabel("2FA (optional):"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        content.add(otpField, gbc);

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
                otpField.setText(selected.getOtpSecret());
            }
        });

        newButton.addActionListener(e -> {
            UserRegistry.User newUser = new UserRegistry.User("Neuer Benutzer", "", "", "");
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
    }

    private void refreshUserList() {
        comboModel.removeAllElements();
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        for (UserRegistry.User user : users) {
            comboModel.addElement(user);
        }
    }
}
