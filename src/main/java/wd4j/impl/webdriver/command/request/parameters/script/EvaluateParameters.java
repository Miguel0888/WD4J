package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.script.ResultOwnership;
import wd4j.impl.webdriver.type.script.Target;
import wd4j.impl.websocket.Command;

public class EvaluateParameters implements Command.Params {
    private final String expression;
    private final Target target;
    private final boolean awaitPromise;
    private final ResultOwnership resultOwnership;
    private final boolean userActivation;

    public EvaluateParameters(String expression, Target target, boolean awaitPromise, ResultOwnership resultOwnership, boolean userActivation) {
        this.expression = expression;
        this.target = target;
        this.awaitPromise = awaitPromise;
        this.resultOwnership = resultOwnership;
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

    public boolean getUserActivation() {
        return userActivation;
    }
}
