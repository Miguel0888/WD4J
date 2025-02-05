package wd4j.impl.webdriver.type.network.parameters;

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
