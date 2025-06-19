package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.type.network.WDAuthCredentials;
import de.bund.zrb.impl.webdriver.type.network.WDRequest;

public class ContinueWithAuthCredentials extends ContinueWithAuthParameters {
    private final Action action = Action.PROVIDE_CREDENTIALS;
    private final WDAuthCredentials credentials;

    public ContinueWithAuthCredentials(WDRequest WDRequest, WDAuthCredentials credentials) {
        super(WDRequest);
        this.credentials = credentials;
    }

    public Action getAction() {
        return action;
    }

    public WDAuthCredentials getCredentials() {
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
