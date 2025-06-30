package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.commandframework.MenuCommand;

public class PlayTestSuiteCommand implements MenuCommand {

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
        System.out.println("Running selected suite...");
        // TODO: Deine Logik hier
    }
}
