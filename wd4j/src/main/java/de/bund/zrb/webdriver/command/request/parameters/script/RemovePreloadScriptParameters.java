package de.bund.zrb.webdriver.command.request.parameters.script;

import de.bund.zrb.webdriver.type.script.WDPreloadScript;
import de.bund.zrb.websocket.WDCommand;

public class RemovePreloadScriptParameters implements WDCommand.Params {
    public final WDPreloadScript script;

    public RemovePreloadScriptParameters(WDPreloadScript script) {
        this.script = script;
    }

    public WDPreloadScript getScript() {
        return script;
    }
}
