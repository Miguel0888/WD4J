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
        TestPlayerService.getInstance().registerLogger(runnerPanel.getLogger());
        tabbedPane.addTab("Runner", runnerPanel);
        tabbedPane.setSelectedComponent(runnerPanel);

        // Test Starten
        new Thread(() -> {
            TestPlayerService.getInstance().runSuites();
        }).start();
    }


}
