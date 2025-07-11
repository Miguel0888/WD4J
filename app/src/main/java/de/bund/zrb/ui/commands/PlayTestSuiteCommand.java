package de.bund.zrb.ui.commands;

import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import java.util.List;

public class PlayTestSuiteCommand extends ShortcutMenuCommand {

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

        List<TestSuite> suitesToRun = TestPlayerService.getInstance().getSuitesToRun();
        if (suitesToRun == null || suitesToRun.isEmpty()) {
            System.out.println("⚠️ Keine Suiten markiert!");
            return;
        }

        TestPlayerService.getInstance().runSuites(suitesToRun);

        System.out.println("✅ Playback beendet");
    }

}
