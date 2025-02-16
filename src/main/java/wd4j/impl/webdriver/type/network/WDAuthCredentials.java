package wd4j.impl.webdriver.type.network;

public class WDAuthCredentials {
    private final String type = "password";
    private final String username;
    private final String password;

    public WDAuthCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}