package de.bund.zrb.ui.commands;


import de.bund.zrb.ui.commandframework.MenuCommand;

public class OpenSettingsCommand implements MenuCommand {

    @Override
    public String getId() {
        return "file.settings";
    }

    @Override
    public String getLabel() {
        return "Einstellungen öffnen";
    }

    @Override
    public void perform() {
        System.out.println("Settings geöffnet...");
        // TODO: Deine Logik hier
    }
}
