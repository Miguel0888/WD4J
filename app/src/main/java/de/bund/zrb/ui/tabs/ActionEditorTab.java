package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    public ActionEditorTab(TestAction action) {
        super("Action: " + action.getAction(), action);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(new JLabel("Action:"), gbc);
        gbc.gridx = 1;
        JTextField actionField = new JTextField(action.getAction());
        add(actionField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Value:"), gbc);
        gbc.gridx = 1;
        JTextField valueField = new JTextField(action.getValue() != null ? action.getValue() : "");
        add(valueField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Selector:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> selectorBox = new JComboBox<String>();
        selectorBox.setEditable(true);
        if (action.getSelectedSelector() != null) {
            selectorBox.addItem(action.getSelectedSelector());
        }
        for (Map.Entry<String, String> entry : action.getLocators().entrySet()) {
            selectorBox.addItem(entry.getValue());
        }
        selectorBox.setSelectedItem(action.getSelectedSelector());
        add(selectorBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Locator Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> locatorTypeBox = new JComboBox<String>(new String[]{
                "css", "xpath", "id", "text", "role", "label", "placeholder", "altText"
        });
        locatorTypeBox.setEditable(true);
        if (action.getLocatorType() != null) {
            locatorTypeBox.setSelectedItem(action.getLocatorType());
        }
        add(locatorTypeBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Timeout (ms):"), gbc);
        gbc.gridx = 1;
        JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        add(timeoutField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton applyBtn = new JButton("Speichern");
        applyBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.setAction(actionField.getText().trim());
                action.setValue(valueField.getText().trim());
                action.setSelectedSelector((String) selectorBox.getSelectedItem());
                action.setLocatorType((String) locatorTypeBox.getSelectedItem());
                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                JOptionPane.showMessageDialog(ActionEditorTab.this, "Änderungen gespeichert.");
            }
        });
        add(applyBtn, gbc);
    }
}
