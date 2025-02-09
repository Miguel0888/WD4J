package wd4j.impl.webdriver.command.request.network.parameters;

import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.websocket.Command;

public class ContinueWithAuthParameters implements Command.Params {
    private final Request request;
    private final Credentials auth; // ToDo: Unclear

    public ContinueWithAuthParameters(Request request, Credentials auth) {
        this.request = request;
        this.auth = auth;
    }

    public Request getRequest() {
        return request;
    }

    public Credentials getAuth() {
        return auth;
    }
}
