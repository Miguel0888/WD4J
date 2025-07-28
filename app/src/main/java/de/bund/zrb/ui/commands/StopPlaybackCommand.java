package de.bund.zrb.ui.commands;

import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class StopPlaybackCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "testsuite.stop";
    }

    @Override
    public String getLabel() {
        return "Playback stoppen";
    }

    @Override
    public void perform() {
        TestPlayerService.getInstance().stopPlayback();
    }
}
