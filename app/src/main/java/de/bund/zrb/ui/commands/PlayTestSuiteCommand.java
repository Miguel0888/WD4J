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

        TestPlayerService.getInstance().runSuites();

        System.out.println("✅ Playback beendet");
    }

}
