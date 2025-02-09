package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.PreloadScript;
import wd4j.impl.websocket.Command;

public class RemovePreloadScriptParameters implements Command.Params {
    public final PreloadScript script;

    public RemovePreloadScriptParameters(PreloadScript script) {
        this.script = script;
    }

    public PreloadScript getScript() {
        return script;
    }
}
