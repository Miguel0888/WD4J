package de.bund.zrb.webdriver.command.request.parameters.script;

import com.google.gson.annotations.SerializedName;
import de.bund.zrb.webdriver.type.script.WDLocalValue;
import de.bund.zrb.webdriver.type.script.WDResultOwnership;
import de.bund.zrb.webdriver.type.script.WDSerializationOptions;
import de.bund.zrb.webdriver.type.script.WDTarget;
import de.bund.zrb.websocket.WDCommand;

import java.util.List;

public class CallFunctionParameters implements WDCommand.Params {
    private final String functionDeclaration;
    private final boolean awaitPromise;
    private final WDTarget target;
    private final List<WDLocalValue> arguments; // Optional
    private final WDResultOwnership resultOwnership; // Optional
    private final WDSerializationOptions serializationOptions; // Optional
    @SerializedName("this") // 🔥 Explizit auf "this" in JSON mappen, da "this" ein reserviertes Schlüsselwort in Java ist
    private final WDLocalValue thisObject; // Optional
    private final boolean userActivation;  // Optional, default false

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget target) {
        this(functionDeclaration, awaitPromise, target, null, null, null, null, false);
    }

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments) {
        this(functionDeclaration, awaitPromise, target, arguments, null, null, null, false);
    }

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDLocalValue thisObject) {
        this(functionDeclaration, awaitPromise, target, arguments, null, null, thisObject, false);
    }

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, WDLocalValue thisObject) {
        this(functionDeclaration, awaitPromise, target, arguments, resultOwnership, serializationOptions, thisObject, false);
    }

    public CallFunctionParameters(String functionDeclaration, boolean awaitPromise, WDTarget target, List<WDLocalValue> arguments, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, WDLocalValue thisObject, boolean userActivation) {
        this.functionDeclaration = functionDeclaration;
        this.awaitPromise = awaitPromise;
        this.target = target;
        this.arguments = arguments;
        this.resultOwnership = resultOwnership;
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

    public WDTarget getTarget() {
        return target;
    }

    public List<WDLocalValue> getArguments() {
        return arguments;
    }

    public WDResultOwnership getResultOwnership() {
        return resultOwnership;
    }

    public WDSerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public WDLocalValue getThisObject() {
        return thisObject;
    }

    public boolean isUserActivation() {
        return userActivation;
    }
}
