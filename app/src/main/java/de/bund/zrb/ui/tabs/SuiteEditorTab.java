package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.ThenExpectation;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class SuiteEditorTab extends AbstractEditorTab<TestSuite> {

    private final DefaultListModel<Object> setupModel = new DefaultListModel<>();
    private final DefaultListModel<Object> teardownModel = new DefaultListModel<>();
    private final JPanel setupDetailPanel = new JPanel(new BorderLayout());
    private final JPanel teardownDetailPanel = new JPanel(new BorderLayout());
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
        JSplitPane setupSplit = createStepPanel(
                "Setup (Given)", suite.getGiven(), setupModel, setupDetailPanel, true);
        // Teardown (Then)
        JSplitPane teardownSplit = createStepPanel(
                "Teardown (Then)", suite.getThen(), teardownModel, teardownDetailPanel, false);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, setupSplit, teardownSplit);
        mainSplit.setResizeWeight(0.5);

        form.add(mainSplit, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                suite.setName(nameField.getText());
                suite.setDescription(descriptionArea.getText());
                TestRegistry.getInstance().save();
                ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suite.getName()));
            }
        });

        add(form, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
    }

    private JSplitPane createStepPanel(String title, List<?> steps, DefaultListModel<Object> model,
                                       JPanel detailPanel, boolean isGiven) {
        model.clear();
        for (Object step : steps) {
            model.addElement(step);
        }

        JList<Object> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer());

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Object selected = list.getSelectedValue();
                updateDetailPanel(selected, detailPanel, isGiven);
            }
        });

        JButton addBtn = new JButton("+");
        JButton removeBtn = new JButton("🗑");

        addBtn.addActionListener(e -> {
            Object step = isGiven ? new GivenCondition() : new ThenExpectation();
            if (isGiven) {
                getModel().getGiven().add((GivenCondition) step);
            } else {
                getModel().getThen().add((ThenExpectation) step);
            }
            model.addElement(step);
            TestRegistry.getInstance().save();
            ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
        });

        removeBtn.addActionListener(e -> {
            int selected = list.getSelectedIndex();
            if (selected >= 0) {
                Object item = model.getElementAt(selected);
                if (isGiven) {
                    getModel().getGiven().remove(item);
                } else {
                    getModel().getThen().remove(item);
                }
                model.removeElement(item);
                detailPanel.removeAll();
                detailPanel.revalidate();
                detailPanel.repaint();
                TestRegistry.getInstance().save();
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

        detailPanel.removeAll();
        detailPanel.setBorder(BorderFactory.createTitledBorder("Details"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panel, detailPanel);
        split.setResizeWeight(0.4);
        return split;
    }

    private void updateDetailPanel(Object selected, JPanel targetPanel, boolean isGiven) {
        targetPanel.removeAll();

        if (isGiven && selected instanceof GivenCondition) {
            targetPanel.add(new JLabel("Given editor (TODO)"), BorderLayout.CENTER);
        } else if (!isGiven && selected instanceof ThenExpectation) {
            targetPanel.add(new ThenExpectationEditorTab((ThenExpectation) selected), BorderLayout.CENTER);
        }

        targetPanel.revalidate();
        targetPanel.repaint();
    }
}
