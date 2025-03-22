package wd4j.impl.dto.command.request.parameters.script;

import wd4j.impl.dto.type.script.WDResultOwnership;
import wd4j.impl.dto.type.script.WDSerializationOptions;
import wd4j.impl.dto.type.script.WDTarget;
import wd4j.impl.websocket.WDCommand;

public class EvaluateParameters implements WDCommand.Params {
    private final String expression;
    private final WDTarget WDTarget;
    private final boolean awaitPromise;
    private final WDResultOwnership WDResultOwnership; // Optional
    private final WDSerializationOptions WDSerializationOptions; // Optional
    private final boolean userActivation; // Optional, default false

    public EvaluateParameters(String expression, WDTarget WDTarget, boolean awaitPromise) {
        this(expression, WDTarget, awaitPromise, null, null, false);
    }

    public EvaluateParameters(String expression, WDTarget WDTarget, boolean awaitPromise, WDResultOwnership WDResultOwnership, WDSerializationOptions WDSerializationOptions, boolean userActivation) {
        this.expression = expression;
        this.WDTarget = WDTarget;
        this.awaitPromise = awaitPromise;
        this.WDResultOwnership = WDResultOwnership;
        this.WDSerializationOptions = WDSerializationOptions;
        this.userActivation = userActivation;
    }

    public String getExpression() {
        return expression;
    }

    public WDTarget getTarget() {
        return WDTarget;
    }

    public boolean getAwaitPromise() {
        return awaitPromise;
    }

    public WDResultOwnership getResultOwnership() {
        return WDResultOwnership;
    }

    public WDSerializationOptions getSerializationOptions() {
        return WDSerializationOptions;
    }

    public boolean getUserActivation() {
        return userActivation;
    }
}
