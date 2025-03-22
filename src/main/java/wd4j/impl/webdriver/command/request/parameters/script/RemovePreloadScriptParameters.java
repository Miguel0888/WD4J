package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.WDPreloadScript;
import wd4j.impl.websocket.WDCommand;

public class RemovePreloadScriptParameters implements WDCommand.Params {
    public final WDPreloadScript script;

    public RemovePreloadScriptParameters(WDPreloadScript script) {
        this.script = script;
    }

    public WDPreloadScript getScript() {
        return script;
    }
}
