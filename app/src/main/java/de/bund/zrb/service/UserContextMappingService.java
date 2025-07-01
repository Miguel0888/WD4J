package de.bund.zrb.service;

import java.util.HashMap;
import java.util.Map;

public class UserContextMappingService {

    private static final UserContextMappingService INSTANCE = new UserContextMappingService();

    private final Map<String, UserRegistry.User> contextUserMap = new HashMap<>();

    // aktuell ausgewählter Benutzer
    private UserRegistry.User currentUser;

    public static UserContextMappingService getInstance() {
        return INSTANCE;
    }

    public void bindUserToContext(String contextId, UserRegistry.User user) {
        contextUserMap.put(contextId, user);
    }

    public UserRegistry.User getUserForContext(String contextId) {
        return contextUserMap.get(contextId);
    }

    public void remove(String contextId) {
        contextUserMap.remove(contextId);
    }

    public UserRegistry.User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserRegistry.User user) {
        this.currentUser = user;
    }
}
