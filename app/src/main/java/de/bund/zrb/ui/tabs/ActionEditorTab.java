package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    public ActionEditorTab(TestAction action) {
        super("Edit Action", action);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // bekannte Aktionen: zusätzlich check, radio, screenshot
        Set<String> knownActions = new TreeSet<>(Arrays.asList(
                "click", "input", "select", "check", "radio", "screenshot"
        ));

        Set<String> locatorTypes = new TreeSet<>(Arrays.asList(
                "xpath", "css", "id", "role", "text", "label"
        ));

        // Felder
        formPanel.add(new JLabel("Action:"));
        JComboBox<String> actionBox = new JComboBox<>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        formPanel.add(new JLabel("Value:"));
        JComboBox<String> valueBox = new JComboBox<>(new String[]{ "OTP" });
        valueBox.setEditable(true);
        valueBox.setSelectedItem(action.getValue());
        formPanel.add(valueBox);

        formPanel.add(new JLabel("Locator Type:"));
        JComboBox<String> locatorBox = new JComboBox<>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);
        locatorBox.setSelectedItem(action.getLocatorType());
        formPanel.add(locatorBox);

        formPanel.add(new JLabel("Selector:"));
        JComboBox<String> selectorBox = new JComboBox<>();
        selectorBox.setEditable(true);
        if (action.getSelectedSelector() != null) {
            selectorBox.addItem(action.getSelectedSelector());
        }
        for (String sel : action.getLocators().values()) {
            if (sel != null && !sel.trim().isEmpty() && selectorBox.getItemCount() < 10) {
                selectorBox.addItem(sel);
            }
        }
        selectorBox.setSelectedItem(action.getSelectedSelector());
        formPanel.add(selectorBox);

        formPanel.add(new JLabel("User:"));
        // Alle bekannten Benutzer für Multi-User-Szenarien
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(UserRegistry.User::getUsername)
                .toArray(String[]::new);
        JComboBox<String> userBox = new JComboBox<>(users);
        userBox.setSelectedItem(action.getUser());
        formPanel.add(userBox);

        formPanel.add(new JLabel("Timeout (ms):"));
        JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        add(formPanel, BorderLayout.NORTH);

        // Speichern-Button
        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.setAction((String) actionBox.getSelectedItem());
                action.setValue((String) valueBox.getSelectedItem());
                action.setLocatorType((String) locatorBox.getSelectedItem());
                action.setSelectedSelector((String) selectorBox.getSelectedItem());
                action.setUser((String) userBox.getSelectedItem());
                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(ActionEditorTab.this, "Änderungen gespeichert.");
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }
}
