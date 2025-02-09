package wd4j.impl.webdriver.command.request.script.parameters;

import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.script.ChannelValue;
import wd4j.impl.websocket.Command;

import java.util.List;

public class AddPreloadScriptParameters implements Command.Params {
    private final String functionDeclaration;
    private final List<ChannelValue> arguements;
    private final List<BrowsingContext> browsingContexts;
    private final List<UserContext> userContexts;
    private final String sandbox;

    public AddPreloadScriptParameters(String functionDeclaration, List<ChannelValue> arguements, List<BrowsingContext> browsingContexts, List<UserContext> userContexts, String sandbox) {
        this.functionDeclaration = functionDeclaration;
        this.arguements = arguements;
        this.browsingContexts = browsingContexts;
        this.userContexts = userContexts;
        this.sandbox = sandbox;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public List<ChannelValue> getArguements() {
        return arguements;
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
