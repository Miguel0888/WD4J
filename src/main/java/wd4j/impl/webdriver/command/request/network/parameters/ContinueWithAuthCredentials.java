package wd4j.impl.webdriver.command.request.network.parameters;

import wd4j.impl.webdriver.type.network.AuthCredentials;

public class ContinueWithAuthCredentials implements Credentials {
    private final String action = "provideCredentials";
    private final AuthCredentials credentials;

    public ContinueWithAuthCredentials(AuthCredentials credentials) {
        this.credentials = credentials;
    }

    public String getAction() {
        return action;
    }

    public AuthCredentials getCredentials() {
        return credentials;
    }
}
