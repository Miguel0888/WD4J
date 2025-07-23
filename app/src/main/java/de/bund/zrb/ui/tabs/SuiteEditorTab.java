package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.ThenExpectation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class SuiteEditorTab extends AbstractEditorTab<TestSuite> {

    private final DefaultListModel<Object> setupModel = new DefaultListModel<>();
    private final DefaultListModel<Object> teardownModel = new DefaultListModel<>();
    private final JTextField nameField;
    private final JTextArea descriptionArea;

    public SuiteEditorTab(TestSuite suite) {
        super("Suite: " + suite.getName(), suite);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel namePanel = new JPanel(new GridLayout(0, 2, 8, 8));
        namePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        nameField = new JTextField(suite.getName());
        descriptionArea = new JTextArea(suite.getDescription(), 3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        namePanel.add(new JLabel("Name:"));
        namePanel.add(nameField);
        namePanel.add(new JLabel("Beschreibung:"));
        namePanel.add(new JScrollPane(descriptionArea));

        form.add(namePanel, BorderLayout.NORTH);

        // Setup (Given)
        JPanel setupPanel = createStepPanel("Setup (Given)", suite.getGiven(), setupModel);
        // Teardown (Then)
        JPanel teardownPanel = createStepPanel("Teardown (Then)", suite.getThen(), teardownModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, setupPanel, teardownPanel);
        splitPane.setResizeWeight(0.5);

        form.add(splitPane, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                suite.setName(nameField.getText());
                suite.setDescription(descriptionArea.getText());
                ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suite.getName()));
            }
        });

        add(form, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
    }

    private JPanel createStepPanel(String title, List<?> steps, DefaultListModel<Object> model) {
        model.clear();
        for (Object step : steps) {
            model.addElement(step);
        }

        JList<Object> list = new JList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer());

        JButton addBtn = new JButton("+");
        JButton removeBtn = new JButton("🗑");

        addBtn.addActionListener(e -> {
            if (title.contains("Setup")) {
                GivenCondition step = new GivenCondition();
                getModel().getGiven().add(step);
                model.addElement(step);
            } else {
                ThenExpectation step = new ThenExpectation();
                getModel().getThen().add(step);
                model.addElement(step);
            }
            ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
        });

        removeBtn.addActionListener(e -> {
            int selected = list.getSelectedIndex();
            if (selected >= 0) {
                Object item = model.getElementAt(selected);
                if (title.contains("Setup")) {
                    getModel().getGiven().remove(item);
                } else {
                    getModel().getThen().remove(item);
                }
                model.removeElement(item);
                ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tools.add(addBtn);
        tools.add(removeBtn);

        panel.add(tools, BorderLayout.SOUTH);
        return panel;
    }
}
