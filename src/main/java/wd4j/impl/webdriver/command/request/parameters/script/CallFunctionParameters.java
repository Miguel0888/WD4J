package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.LocalValue;
import wd4j.impl.webdriver.type.script.SerializationOptions;
import wd4j.impl.webdriver.type.script.Target;
import wd4j.impl.websocket.Command;

import java.util.List;

public class CallFunctionParameters implements Command.Params {
    private final String functionDeclaration;
    private final boolean awaitPromise;
    private final Target target;
    private final List<LocalValue> arguments;
    private final SerializationOptions serializationOptions;
    private final LocalValue thisObject;
    private final boolean userActivation;

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, Target target, List<LocalValue> arguments, SerializationOptions serializationOptions, LocalValue thisObject, boolean userActivation) {
        this.functionDeclaration = functionDeclaration;
        this.awaitPromise = awaitPromise;
        this.target = target;
        this.arguments = arguments;
        this.serializationOptions = serializationOptions;
        this.thisObject = thisObject;
        this.userActivation = userActivation;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public boolean isAwaitPromise() {
        return awaitPromise;
    }

    public Target getTarget() {
        return target;
    }

    public List<LocalValue> getArguments() {
        return arguments;
    }

    public SerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public LocalValue getThisObject() {
        return thisObject;
    }

    public boolean isUserActivation() {
        return userActivation;
    }
}
