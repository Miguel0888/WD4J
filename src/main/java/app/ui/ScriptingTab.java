package app.ui;

import javax.swing.*;
import java.awt.*;

public class ScriptingTab {
    private JPanel panel;
    private JTextArea scriptLog;

    public ScriptingTab() {
        scriptLog = new JTextArea();
        scriptLog.setEditable(false);

        JScrollPane scriptScrollPane = new JScrollPane(scriptLog);
        panel = new JPanel(new BorderLayout());
        panel.add(scriptScrollPane, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getScriptLog() {
        return scriptLog;
    }
}
