package wd4j.impl.webdriver.command.request.parameters.network;

public enum ContinueWithAuthNoCredentials implements Credentials {
    DEFAULT( "default" ),
    CANCEL( "cancel" );

    private final String value;

    private ContinueWithAuthNoCredentials( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
