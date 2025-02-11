package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.script.ChannelValue;
import wd4j.impl.websocket.Command;

import java.util.List;

public class AddPreloadScriptParameters implements Command.Params {
    private final String functionDeclaration;
    private final List<ChannelValue> arguments; // Optional
    private final List<BrowsingContext> browsingContexts; // Optional
    private final List<UserContext> userContexts; // Optional
    private final String sandbox; // Optional

    public AddPreloadScriptParameters(String functionDeclaration) {
        this(functionDeclaration, null, null, null, null);
    }

    public AddPreloadScriptParameters(String functionDeclaration, List<ChannelValue> arguments, List<BrowsingContext> browsingContexts, List<UserContext> userContexts, String sandbox) {
        this.functionDeclaration = functionDeclaration;
        this.arguments = arguments;
        this.browsingContexts = browsingContexts;
        this.userContexts = userContexts;
        this.sandbox = sandbox;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public List<ChannelValue> getArguments() {
        return arguments;
    }

    public List<BrowsingContext> getBrowsingContexts() {
        return browsingContexts;
    }

    public List<UserContext> getUserContexts() {
        return userContexts;
    }

    public String getSandbox() {
        return sandbox;
    }
}
