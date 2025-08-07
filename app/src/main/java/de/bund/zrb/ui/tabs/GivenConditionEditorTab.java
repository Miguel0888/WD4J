package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.Code;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class GivenConditionEditorTab extends JPanel {

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        typeBox = new JComboBox<>(GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new));
        typeBox.setSelectedItem(condition.getType());
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

        rebuildDynamicForm(condition.getType());
    }

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) return;

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        for (GivenField field : def.getFields().values()) {
            Object value = paramMap.getOrDefault(field.name, field.defaultValue);
            JComponent input;

            if (field.type == Code.class) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? value.toString() : "");

                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                // Label (volle Breite)
                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                // Editor (volle Breite, wächst)
                gbc.gridy = row++;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1.0;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor);
                continue;
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

            JTextField tf = new JTextField(value != null ? value.toString() : "");
            input = tf;
            dynamicFieldsPanel.add(input, gbc);

            inputs.put(field.name, input);
            row++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        condition.setType(selectedType);

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            }
        }

        condition.setValue(serializeValueMap(result));
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    private String serializeValueMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }
}
