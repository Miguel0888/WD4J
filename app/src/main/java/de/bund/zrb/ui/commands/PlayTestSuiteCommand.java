package de.bund.zrb.ui.commands;

import com.microsoft.playwright.Browser;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.TestSuitePlayer;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

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

        TestSuite suite = TestRegistry.getInstance().getAll().get(0); // TODO: Aus UI nehmen!
        TestSuitePlayer player = new TestSuitePlayer();

        player.runSuite(suite);

        System.out.println("✅ Playback beendet");
    }


}
