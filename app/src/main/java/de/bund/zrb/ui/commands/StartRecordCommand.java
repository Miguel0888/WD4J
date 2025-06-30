package de.bund.zrb.ui.commands;


import de.bund.zrb.ui.commandframework.MenuCommand;

public class StartRecordCommand implements MenuCommand {

    @Override
    public String getId() {
        return "record.start";
    }

    @Override
    public String getLabel() {
        return "Aufzeichnung starten";
    }

    @Override
    public void perform() {
        System.out.println("Recording started...");
        // TODO: Deine Logik hier
    }
}
