package wd4j.impl.webdriver.type.network.parameters;

import wd4j.impl.webdriver.type.network.Intercept;
import wd4j.impl.websocket.Command;

public class RemoveInterceptParameters implements Command.Params {
    private final Intercept intercept;

    public RemoveInterceptParameters(Intercept intercept) {
        this.intercept = intercept;
    }

    public Intercept getIntercept() {
        return intercept;
    }
}
