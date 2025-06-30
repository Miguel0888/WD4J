package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class RightDrawer extends JPanel {

    private final ActionTable actionTable;

    public RightDrawer() {
        super(new BorderLayout());

        this.actionTable = new ActionTable();

        JButton recordBtn = new JButton("● Aufnahme starten");
        recordBtn.setForeground(Color.RED);
        recordBtn.addActionListener(e -> startRecording());

        JButton saveBtn = new JButton("💾 Speichern als Testsuite");
        saveBtn.addActionListener(e -> saveAsTestSuite());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(recordBtn);
        topPanel.add(saveBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(actionTable), BorderLayout.CENTER);
    }

    private void startRecording() {
        System.out.println("Recording started (Demo) ...");
        // TODO: Hook in echten Browser-Recorder → z. B. BrowserService.onAction(action -> addAction)
        // Demo: Dummy-Aktion hinzufügen
        TestAction action = new TestAction();
        action.setAction("click");
        action.setSelectedSelector("#demoButton");
        action.setValue("Demo-Wert");
        actionTable.addAction(action);
    }

    private void saveAsTestSuite() {
        List<TestAction> actions = actionTable.tableModel.getActions();

        String suiteName = JOptionPane.showInputDialog(this, "Name der Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (suiteName == null || suiteName.trim().isEmpty()) return;

        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setName("TestCase-" + suiteName);
        testCase.setThen(actions);

        TestSuite suite = new TestSuite();
        suite.setId(UUID.randomUUID().toString());
        suite.setName(suiteName);
        suite.getTestCases().add(testCase);

        // Speichere Testsuite JSON
        SettingsService.getInstance().save("testsuites/" + suiteName + ".json", suite);

        JOptionPane.showMessageDialog(this, "Testsuite gespeichert: " + suiteName);
    }
}
