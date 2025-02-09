package wd4j.impl.webdriver.command.request.parameters.network;

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
