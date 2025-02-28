package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.WDLocalValue;
import wd4j.impl.webdriver.type.script.WDSerializationOptions;
import wd4j.impl.webdriver.type.script.WDTarget;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class CallFunctionParameters<T> implements WDCommand.Params {
    private final String functionDeclaration;
    private final boolean awaitPromise;
    private final WDTarget WDTarget;
    private final List<WDLocalValue> arguments; // Optional
    private final WDSerializationOptions WDSerializationOptions; // Optional
    private final WDLocalValue thisObject; // Optional
    private final boolean userActivation;  // Optional, default false

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget) {
        this(functionDeclaration, awaitPromise, WDTarget, null, null, null, false);
    }

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget WDTarget, List<WDLocalValue> arguments, WDSerializationOptions WDSerializationOptions, WDLocalValue thisObject, boolean userActivation) {
        this.functionDeclaration = functionDeclaration;
        this.awaitPromise = awaitPromise;
        this.WDTarget = WDTarget;
        this.arguments = arguments;
        this.WDSerializationOptions = WDSerializationOptions;
        this.thisObject = thisObject;
        this.userActivation = userActivation;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public boolean isAwaitPromise() {
        return awaitPromise;
    }

    public WDTarget getTarget() {
        return WDTarget;
    }

    public List<WDLocalValue> getArguments() {
        return arguments;
    }

    public WDSerializationOptions getSerializationOptions() {
        return WDSerializationOptions;
    }

    public WDLocalValue getThisObject() {
        return thisObject;
    }

    public boolean isUserActivation() {
        return userActivation;
    }
}
