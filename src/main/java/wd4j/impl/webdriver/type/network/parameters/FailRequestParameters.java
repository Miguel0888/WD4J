package wd4j.impl.webdriver.type.network.parameters;

import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.websocket.Command;

public class FailRequestParameters implements Command.Params {
    private final Request request;

    public FailRequestParameters(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
