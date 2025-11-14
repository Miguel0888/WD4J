package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class CaptureScreenshotCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "browser.captureScreenshot";
    }

    @Override
    public String getLabel() {
        return "Screenshot aufnehmen";
    }

    @Override
    public void perform() {
        // Use the new convenience method: persists to report, logs, then shows a window.
        ToolsRegistry.getInstance()
                .screenshotTool()
                .captureAndShowInWindow("Screenshot");
    }
}
