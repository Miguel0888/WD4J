package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.ThenExpectation;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ThenExpectationEditorTab extends JPanel {

    private final JComboBox<String> typeBox;
    private final JTextField valueField;

    public ThenExpectationEditorTab(ThenExpectation expectation) {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] predefinedTypes = {
                "screenshot",
                "checkText",
                "elementExists",
                "checkTitle"
        };

        typeBox = new JComboBox<>(predefinedTypes);
        typeBox.setEditable(true);
        typeBox.setSelectedItem(expectation.getType());

        valueField = new JTextField(expectation.getValue());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Typ:"));
        form.add(typeBox);
        form.add(new JLabel("Wert:"));
        form.add(valueField);

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expectation.setType((String) typeBox.getSelectedItem());
                expectation.setValue(valueField.getText());
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(ThenExpectationEditorTab.this, "Änderungen gespeichert.");
            }
        });

        add(form, BorderLayout.NORTH);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(saveButton);
        add(south, BorderLayout.SOUTH);
    }
}
