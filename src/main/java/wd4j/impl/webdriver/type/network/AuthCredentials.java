package wd4j.impl.webdriver.type.network;

public class AuthCredentials {
    private final String username;
    private final String password;

    public AuthCredentials(String username, String password) {
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