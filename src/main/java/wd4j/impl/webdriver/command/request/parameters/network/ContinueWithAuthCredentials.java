package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.AuthCredentials;
import wd4j.impl.webdriver.type.network.Request;

public class ContinueWithAuthCredentials extends ContinueWithAuthParameters {
    private final Action action = Action.PROVIDE_CREDENTIALS;
    private final AuthCredentials credentials;

    public ContinueWithAuthCredentials(Request request, AuthCredentials credentials) {
        super(request);
        this.credentials = credentials;
    }

    public Action getAction() {
        return action;
    }

    public AuthCredentials getCredentials() {
        return credentials;
    }

    public enum Action implements ContinueWithAuthParameters.Action {
        PROVIDE_CREDENTIALS("provideCredentials");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        @Override // confirmed design
        public String value() {
            return value;
        }
    }
}
