package de.bund.zrb.webdriver.command.request.parameters.script;

import de.bund.zrb.webdriver.type.script.WDResultOwnership;
import de.bund.zrb.webdriver.type.script.WDSerializationOptions;
import de.bund.zrb.webdriver.type.script.WDTarget;
import de.bund.zrb.websocket.WDCommand;

public class EvaluateParameters implements WDCommand.Params {
    private final String expression;
    private final WDTarget WDTarget;
    private final boolean awaitPromise;
    private final WDResultOwnership WDResultOwnership; // Optional
    private final WDSerializationOptions WDSerializationOptions; // Optional
    private final boolean userActivation; // Optional, default false

    public EvaluateParameters(String expression, WDTarget target, boolean awaitPromise) {
        this(expression, target, awaitPromise, null, null, false);
    }

    public EvaluateParameters(String expression, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions) {
        this(expression, target, awaitPromise, resultOwnership, serializationOptions, false);
    }

    public EvaluateParameters(String expression, WDTarget target, boolean awaitPromise, WDResultOwnership resultOwnership, WDSerializationOptions serializationOptions, boolean userActivation) {
        this.expression = expression;
        this.WDTarget = target;
        this.awaitPromise = awaitPromise;
        this.WDResultOwnership = resultOwnership;
        this.WDSerializationOptions = serializationOptions;
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
