package wd4j.impl.webdriver.type.browser;

// ToDo: How to implement this class correctly?
public class UserContextInfo {
    private final UserContext userContext;

    public UserContextInfo(UserContext userContext) {
        if (userContext == null) {
            throw new IllegalArgumentException("UserContext must not be null.");
        }
        this.userContext = userContext;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}