package wd4j.impl.module.type;

public class NetworkAuthCredentials {
    private final String username;
    private final String password;

    public NetworkAuthCredentials(String username, String password) {
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