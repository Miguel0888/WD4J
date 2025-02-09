package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.websocket.Command;

public abstract class ContinueWithAuthParameters implements Command.Params {
    private final Request request;

    public ContinueWithAuthParameters(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }

    public interface Action extends EnumWrapper {

    }
}
