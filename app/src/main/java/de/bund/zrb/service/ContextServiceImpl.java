package de.bund.zrb.service;

import java.util.*;

public class ContextServiceImpl implements ContextService {
    private static final ContextServiceImpl INSTANCE = new ContextServiceImpl();

    // Mapping: user → browsingContextIds
    private final Map<String, List<String>> userBrowsingContexts = new HashMap<>();

    // Aktuell selektierter User
    private String currentUser = "default";

    // Aktuell selektierter BrowsingContext pro User
    private final Map<String, String> currentContexts = new HashMap<>();

    private ContextServiceImpl() {
        // Standard-User anlegen
        createUserContext(currentUser);
    }

    public static ContextServiceImpl getInstance() {
        return INSTANCE;
    }

    public void createUserContext(String user) {
        if (!userBrowsingContexts.containsKey(user)) {
            userBrowsingContexts.put(user, new ArrayList<>());
            currentContexts.put(user, null);
        }
    }

    public void removeUserContext(String user) {
        userBrowsingContexts.remove(user);
        currentContexts.remove(user);
        if (user.equals(currentUser)) {
            currentUser = "default";
        }
    }

    public List<String> getUserContexts() {
        return new ArrayList<>(userBrowsingContexts.keySet());
    }

    public String getCurrentUserContext() {
        return currentUser;
    }

    public void setCurrentUserContext(String user) {
        if (!userBrowsingContexts.containsKey(user)) {
            throw new IllegalArgumentException("Unknown user context: " + user);
        }
        currentUser = user;
    }

    public void createBrowsingContext(String user) {
        userBrowsingContexts.putIfAbsent(user, new ArrayList<>());
        List<String> contexts = userBrowsingContexts.get(user);

        String newContextId = "context_" + (contexts.size() + 1);
        contexts.add(newContextId);
        currentContexts.put(user, newContextId);
    }

    public void closeBrowsingContext(String user, String contextId) {
        List<String> contexts = userBrowsingContexts.get(user);
        if (contexts != null) {
            contexts.remove(contextId);
            if (contextId.equals(currentContexts.get(user))) {
                currentContexts.put(user, contexts.isEmpty() ? null : contexts.get(0));
            }
        }
    }

    public List<String> getBrowsingContexts(String user) {
        return userBrowsingContexts.getOrDefault(user, Collections.emptyList());
    }

    public String getCurrentBrowsingContext(String user) {
        return currentContexts.get(user);
    }

    public void setCurrentBrowsingContext(String user, String contextId) {
        List<String> contexts = userBrowsingContexts.get(user);
        if (contexts != null && contexts.contains(contextId)) {
            currentContexts.put(user, contextId);
        } else {
            throw new IllegalArgumentException("Unknown context ID: " + contextId + " for user: " + user);
        }
    }
}
