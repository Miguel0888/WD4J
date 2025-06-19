package de.bund.zrb.impl.webdriver.command.request.parameters.script;

import de.bund.zrb.impl.webdriver.type.script.WDPreloadScript;
import de.bund.zrb.impl.websocket.WDCommand;

public class RemovePreloadScriptParameters implements WDCommand.Params {
    public final WDPreloadScript script;

    public RemovePreloadScriptParameters(WDPreloadScript script) {
        this.script = script;
    }

    public WDPreloadScript getScript() {
        return script;
    }
}
