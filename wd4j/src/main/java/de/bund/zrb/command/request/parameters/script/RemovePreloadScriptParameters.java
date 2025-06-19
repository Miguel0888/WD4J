package de.bund.zrb.command.request.parameters.script;

import de.bund.zrb.type.script.WDPreloadScript;
import de.bund.zrb.api.WDCommand;

public class RemovePreloadScriptParameters implements WDCommand.Params {
    public final WDPreloadScript script;

    public RemovePreloadScriptParameters(WDPreloadScript script) {
        this.script = script;
    }

    public WDPreloadScript getScript() {
        return script;
    }
}
