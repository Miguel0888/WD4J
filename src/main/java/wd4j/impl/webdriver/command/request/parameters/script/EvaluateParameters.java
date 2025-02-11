package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.ResultOwnership;
import wd4j.impl.webdriver.type.script.SerializationOptions;
import wd4j.impl.webdriver.type.script.Target;
import wd4j.impl.websocket.Command;

public class EvaluateParameters implements Command.Params {
    private final String expression;
    private final Target target;
    private final boolean awaitPromise;
    private final ResultOwnership resultOwnership; // Optional
    private final SerializationOptions serializationOptions; // Optional
    private final boolean userActivation; // Optional, default false

    public EvaluateParameters(String expression, Target target, boolean awaitPromise) {
        this(expression, target, awaitPromise, null, null, false);
    }

    public EvaluateParameters(String expression, Target target, boolean awaitPromise, ResultOwnership resultOwnership, SerializationOptions serializationOptions, boolean userActivation) {
        this.expression = expression;
        this.target = target;
        this.awaitPromise = awaitPromise;
        this.resultOwnership = resultOwnership;
        this.serializationOptions = serializationOptions;
        this.userActivation = userActivation;
    }

    public String getExpression() {
        return expression;
    }

    public Target getTarget() {
        return target;
    }

    public boolean getAwaitPromise() {
        return awaitPromise;
    }

    public ResultOwnership getResultOwnership() {
        return resultOwnership;
    }

    public SerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public boolean getUserActivation() {
        return userActivation;
    }
}
