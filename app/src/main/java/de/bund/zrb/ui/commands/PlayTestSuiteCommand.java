package de.bund.zrb.ui.commands;

import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.tabs.RunnerPanel;

import javax.swing.*;

public class PlayTestSuiteCommand extends ShortcutMenuCommand {

    private final JTabbedPane tabbedPane;

    public PlayTestSuiteCommand(JTabbedPane tabbedPane) {
        super();
        this.tabbedPane = tabbedPane;
    }

    @Override
    public String getId() {
        return "testsuite.play";
    }

    @Override
    public String getLabel() {
        return "Testsuite ausführen";
    }

    @Override
    public void perform() {
        System.out.println("▶ Starte Playback...");

        // Tab öffnen
        RunnerPanel runnerPanel = new RunnerPanel();
        tabbedPane.addTab("Runner", runnerPanel);
        tabbedPane.setSelectedComponent(runnerPanel);

        // Protokollierung starten
        runnerPanel.appendLog("🟢 Playback gestartet");

        new Thread(() -> {
            TestPlayerService.getInstance().runSuites();
            SwingUtilities.invokeLater(() ->
                    runnerPanel.appendLog("✅ Playback beendet")
            );
        }).start();
    }


}
