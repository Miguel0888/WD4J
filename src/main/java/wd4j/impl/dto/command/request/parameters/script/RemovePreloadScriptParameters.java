package wd4j.impl.dto.command.request.parameters.script;

import wd4j.impl.dto.type.script.WDPreloadScript;
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
