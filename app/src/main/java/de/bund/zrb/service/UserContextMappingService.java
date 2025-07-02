package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;

import java.util.HashMap;
import java.util.Map;

public class UserContextMappingService {

    private static final UserContextMappingService INSTANCE = new UserContextMappingService();

    // Username → Context
    private final Map<String, BrowserContext> userContextMap = new HashMap<>();

    // Username → User
    private final Map<String, UserRegistry.User> userMap = new HashMap<>();

    // aktuell ausgewählter Benutzer
    private UserRegistry.User currentUser;

    private UserContextMappingService() {
        String defaultUser = SettingsService.getInstance().get("defaultUser", String.class);
        if (defaultUser != null) {
            UserRegistry.User user = UserRegistry.getInstance().getAll().stream()
                    .filter(u -> u.getUsername().equals(defaultUser))
                    .findFirst()
                    .orElse(null);
            this.currentUser = user;
            if (user != null) {
                userMap.put(user.getUsername(), user);
            }
        }
    }

    public static UserContextMappingService getInstance() {
        return INSTANCE;
    }

    public void bindUserToContext(String username, BrowserContext context, UserRegistry.User user) {
        userContextMap.put(username, context);
        userMap.put(username, user);
    }

    public BrowserContext getContextForUser(String username) {
        return userContextMap.get(username);
    }

    public UserRegistry.User getUser(String username) {
        return userMap.get(username);
    }

    public void remove(String username) {
        userContextMap.remove(username);
        userMap.remove(username);
    }

    public UserRegistry.User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserRegistry.User user) {
        this.currentUser = user;
    }
}

