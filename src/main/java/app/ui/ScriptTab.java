package app.ui;

import app.controller.MainController;
import wd4j.impl.webdriver.type.script.WDExceptionDetails;

import javax.swing.*;
import java.awt.*;

public class ScriptTab implements UIComponent {
    private JPanel panel;
    private JToolBar toolbar;
    private JCheckBox showSelectorsCheckbox, showDomEventsCheckbox;
    private JButton runScriptButton, clearScriptButton;
    private JTextArea scriptLog;
    private MainController controller;

    public ScriptTab(MainController controller) {
        this.controller = controller;

        // UI-Panel initialisieren
        panel = new JPanel(new BorderLayout());

        // Script-Log-Feld
        scriptLog = new JTextArea();
        scriptLog.setEditable(false);
        JScrollPane scriptScrollPane = new JScrollPane(scriptLog);
        panel.add(scriptScrollPane, BorderLayout.CENTER);

        // Toolbar initialisieren
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        showSelectorsCheckbox = new JCheckBox("Show Selectors");
        showDomEventsCheckbox = new JCheckBox("Show DOM Events");
        runScriptButton = new JButton("Run Script");
        clearScriptButton = new JButton("Clear Events");

        toolbar.add(showSelectorsCheckbox);
        toolbar.add(showDomEventsCheckbox);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(runScriptButton);
        toolbar.add(clearScriptButton);

        // Event-Listener für UI-Elemente hinzufügen
        setupListeners();

        // Panel zusammenstellen
        panel.add(scriptScrollPane, BorderLayout.CENTER);
    }

    private void setupListeners() {
        showSelectorsCheckbox.addActionListener(e ->
                controller.showSelectors(showSelectorsCheckbox.isSelected()));

        showDomEventsCheckbox.addActionListener(e ->
                controller.showDomEvents(showDomEventsCheckbox.isSelected()));

        runScriptButton.addActionListener(e -> {
            String script = scriptLog.getText();
            if (script.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Script is empty!", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                controller.runScript(script);
            }
        });

        clearScriptButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null,
                    "Do you really want to clear the console?",
                    "Clear Console", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                scriptLog.setText("");
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getComponentTitle() {
        return "Script";
    }

    public JToolBar getToolbar() {
        return toolbar;
    }

    public void appendLog(String message) {
        scriptLog.append(message + System.lineSeparator());
    }

    public String getScriptLog() {
        // ToDo: Maybe improve the log in that way that it can be directly used as a script / executed
        return scriptLog.getText();
    }
}
