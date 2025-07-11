package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class CaptureScreenshotCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "tools.captureScreenshot";
    }

    @Override
    public String getLabel() {
        return "Screenshot aufnehmen";
    }

    @Override
    public void perform() {
        ToolsRegistry.getInstance().screenshotTool().captureAndSave();
    }
}
