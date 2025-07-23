package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;

public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    public ActionEditorTab(TestAction action) {
        super("Edit Action", action);

        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Mögliche bekannte Werte (Demo / ggf. dynamisch befüllen)
        Set<String> knownActions = new TreeSet<>();
        knownActions.add("click");
        knownActions.add("input");
        knownActions.add("select");

        Set<String> locatorTypes = new TreeSet<>();
        locatorTypes.add("xpath");
        locatorTypes.add("css");
        locatorTypes.add("id");

        // Action
        formPanel.add(new JLabel("Action:"));
        JComboBox<String> actionBox = new JComboBox<>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        // Value
        formPanel.add(new JLabel("Value:"));
        JTextField valueField = new JTextField(action.getValue());
        formPanel.add(valueField);

        // Locator Type
        formPanel.add(new JLabel("Locator Type:"));
        JComboBox<String> locatorBox = new JComboBox<>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);
        locatorBox.setSelectedItem(action.getLocatorType());
        formPanel.add(locatorBox);

        // Selected Selector
        formPanel.add(new JLabel("Selector:"));
        JTextField selectorField = new JTextField(action.getSelectedSelector());
        formPanel.add(selectorField);

        // Timeout
        formPanel.add(new JLabel("Timeout (ms):"));
        JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        add(formPanel, BorderLayout.NORTH);
    }
}
