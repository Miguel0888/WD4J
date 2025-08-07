package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.Code;
import de.bund.zrb.model.ThenExpectation;
import de.bund.zrb.model.ExpectationRegistry;
import de.bund.zrb.model.ExpectationTypeDefinition;
import de.bund.zrb.model.ExpectationTypeDefinition.ExpectationField;
import de.bund.zrb.service.TestRegistry;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThenExpectationEditorTab extends JPanel {

    private final ThenExpectation expectation;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    public ThenExpectationEditorTab(ThenExpectation expectation) {
        this.expectation = expectation;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lade alle Typen aus dem model.ExpectationRegistry
        typeBox = new JComboBox<>(ExpectationRegistry.getInstance().getAll()
                .stream().map(ExpectationTypeDefinition::getType).toArray(String[]::new));
        typeBox.setEditable(false);
        typeBox.setSelectedItem(expectation.getType());
        typeBox.addActionListener(e -> rebuildDynamicForm((String) typeBox.getSelectedItem()));

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel typeRow = new JPanel(new GridLayout(1, 2, 8, 8));
        typeRow.add(new JLabel("Typ:"));
        typeRow.add(typeBox);
        form.add(typeRow, BorderLayout.NORTH);

        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(this::save);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        rebuildDynamicForm(expectation.getType());
    }

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        Map<String, Object> paramMap = expectation.getParameterMap();
        ExpectationTypeDefinition def = ExpectationRegistry.getInstance().get(type);
        if (def == null) return;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        for (ExpectationField field : def.getFields().values()) {
            Object value = paramMap.getOrDefault(field.name, field.defaultValue);
            JComponent input;

            if (field.type == Code.class) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? value.toString() : "");
                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                // Label (ganze Breite)
                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                // Editor (ganze Breite, dehnt sich aus)
                gbc.gridy = row++;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1.0;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor); // wichtig!
                continue; // nächsten Field verarbeiten
            }

            // Label (linke Spalte)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            // Input (rechte Spalte)
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            if (field.type == Boolean.class) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(Boolean.TRUE.equals(value));
                input = checkBox;
            } else if (field.type == Integer.class || field.type == Double.class) {
                Number number = (value instanceof Number)
                        ? (Number) value
                        : parseNumber(value, field.type);
                input = new JSpinner(new SpinnerNumberModel(
                        number.doubleValue(), 0, 1_000_000, 1));
            } else if (!field.options.isEmpty()) {
                JComboBox<Object> comboBox = new JComboBox<>(field.options.toArray());
                comboBox.setSelectedItem(value);
                input = comboBox;
            } else {
                JTextField tf = new JTextField(value != null ? value.toString() : "");
                input = tf;
            }

            dynamicFieldsPanel.add(input, gbc);
            inputs.put(field.name, input);
            row++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        expectation.setType(selectedType);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            Object value = null;
            if (input instanceof JTextField) {
                value = ((JTextField) input).getText();
            } else if (input instanceof RSyntaxTextArea) {
                value = ((RSyntaxTextArea) input).getText();
            } else if (input instanceof JCheckBox) {
                value = ((JCheckBox) input).isSelected();
            } else if (input instanceof JSpinner) {
                value = ((JSpinner) input).getValue();
            } else if (input instanceof JComboBox) {
                value = ((JComboBox<?>) input).getSelectedItem();
            }

            result.put(name, value);
        }

        expectation.setParameterMap(result);

        // Speichern explizit anstoßen (wichtig, wenn vorher NULL gewesen)
        TestRegistry.getInstance().save();

        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    private Number parseNumber(Object value, Class<?> targetType) {
        try {
            String str = value != null ? value.toString() : "0";
            if (targetType == Integer.class) {
                return Integer.parseInt(str);
            } else if (targetType == Double.class) {
                return Double.parseDouble(str);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }

}
