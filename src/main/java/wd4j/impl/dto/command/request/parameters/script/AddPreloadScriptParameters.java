package wd4j.impl.dto.command.request.parameters.script;

import wd4j.impl.dto.type.browser.WDUserContext;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.script.WDChannelValue;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class AddPreloadScriptParameters implements WDCommand.Params {
    private final String functionDeclaration;
    private final List<WDChannelValue> arguments; // Optional
    private final List<WDBrowsingContext> contexts; // Optional
    private final List<WDUserContext> userContexts; // Optional
    private final String sandbox; // Optional

    public AddPreloadScriptParameters(String functionDeclaration) {
        this(functionDeclaration, null, null, null, null);
    }

    public AddPreloadScriptParameters(String functionDeclaration, List<WDChannelValue> arguments) {
        this(functionDeclaration, arguments, null, null, null);
    }

    public AddPreloadScriptParameters(String functionDeclaration, List<WDChannelValue> arguments, List<WDBrowsingContext> contexts) {
        this(functionDeclaration, arguments, contexts, null, null);
    }

    public AddPreloadScriptParameters(String functionDeclaration, List<WDChannelValue> arguments, List<WDBrowsingContext> contexts, List<WDUserContext> userContexts, String sandbox) {
        this.functionDeclaration = functionDeclaration;
        this.arguments = arguments;
        this.contexts = contexts;
        this.userContexts = userContexts;
        this.sandbox = sandbox;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public List<WDChannelValue> getArguments() {
        return arguments;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }

    public List<WDUserContext> getUserContexts() {
        return userContexts;
    }

    public String getSandbox() {
        return sandbox;
    }
}
