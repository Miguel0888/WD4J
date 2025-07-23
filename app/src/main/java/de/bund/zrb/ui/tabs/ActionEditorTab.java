package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.TreeSet;

public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    public ActionEditorTab(TestAction action) {
        super("Edit Action", action);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Set<String> knownActions = new TreeSet<>();
        knownActions.add("click");
        knownActions.add("input");
        knownActions.add("select");

        Set<String> locatorTypes = new TreeSet<>();
        locatorTypes.add("xpath");
        locatorTypes.add("css");
        locatorTypes.add("id");
        locatorTypes.add("role");
        locatorTypes.add("text");
        locatorTypes.add("label");

        // Felder
        formPanel.add(new JLabel("Action:"));
        JComboBox<String> actionBox = new JComboBox<>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        formPanel.add(new JLabel("Value:"));
        JTextField valueField = new JTextField(action.getValue());
        formPanel.add(valueField);

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

        formPanel.add(new JLabel("Timeout (ms):"));
        JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        add(formPanel, BorderLayout.NORTH);

        // Speichern-Button
        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.setAction((String) actionBox.getSelectedItem());
                action.setValue(valueField.getText().trim());
                action.setLocatorType((String) locatorBox.getSelectedItem());
                action.setSelectedSelector((String) selectorBox.getSelectedItem());
                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                JOptionPane.showMessageDialog(ActionEditorTab.this, "Änderungen gespeichert.");
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }
}
