package wd4j.impl.webdriver.command.response.script;

import wd4j.impl.markerInterfaces.resultData.ScriptResult;
import wd4j.impl.webdriver.type.script.PreloadScript;

public class AddPreloadScritpResult implements ScriptResult {
    private PreloadScript script;

    public AddPreloadScritpResult(PreloadScript script) {
        this.script = script;
    }

    public PreloadScript getScript() {
        return script;
    }
}
