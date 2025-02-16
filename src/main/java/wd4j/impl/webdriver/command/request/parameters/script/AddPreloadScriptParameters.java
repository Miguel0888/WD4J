package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDChannelValue;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class AddPreloadScriptParameters implements WDCommand.Params {
    private final String functionDeclaration;
    private final List<WDChannelValue> arguments; // Optional
    private final List<WDBrowsingContext> WDBrowsingContexts; // Optional
    private final List<WDUserContext> WDUserContexts; // Optional
    private final String sandbox; // Optional

    public AddPreloadScriptParameters(String functionDeclaration) {
        this(functionDeclaration, null, null, null, null);
    }

    public AddPreloadScriptParameters(String functionDeclaration, List<WDChannelValue> arguments, List<WDBrowsingContext> WDBrowsingContexts, List<WDUserContext> WDUserContexts, String sandbox) {
        this.functionDeclaration = functionDeclaration;
        this.arguments = arguments;
        this.WDBrowsingContexts = WDBrowsingContexts;
        this.WDUserContexts = WDUserContexts;
        this.sandbox = sandbox;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public List<WDChannelValue> getArguments() {
        return arguments;
    }

    public List<WDBrowsingContext> getBrowsingContexts() {
        return WDBrowsingContexts;
    }

    public List<WDUserContext> getUserContexts() {
        return WDUserContexts;
    }

    public String getSandbox() {
        return sandbox;
    }
}
