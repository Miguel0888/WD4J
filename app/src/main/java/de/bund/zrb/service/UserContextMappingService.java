package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;

import java.util.HashMap;
import java.util.List;
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

    /** Setzt den aktuellen Benutzer und persistiert ihn als defaultUser (null entfernt den Eintrag). */
    public synchronized void setCurrentUser(UserRegistry.User user) {
        this.currentUser = user;
        if (user == null) {
            SettingsService.getInstance().set("defaultUser", null);
        } else {
            SettingsService.getInstance().set("defaultUser", user.getUsername());
        }
    }

    /** Komfort-Helfer: liefert aktuellen Usernamen oder null. */
    public String getCurrentUsernameOrNull() {
        return (currentUser == null) ? null : currentUser.getUsername();
    }

    /**
     * Zyklisches Weiterschalten in der Reihenfolge der UserRegistry-Liste.
     * Zyklus: &lt;Keinen&gt; → user0 → user1 → … → &lt;Keinen&gt;.
     * @return der neue aktuelle Benutzer, oder null für &lt;Keinen&gt;
     */
    public synchronized UserRegistry.User cycleNextUser() {
        List<UserRegistry.User> list = UserRegistry.getInstance().getAll();
        if (list == null || list.isEmpty()) {
            setCurrentUser(null);
            return null;
        }

        int idx = -1; // -1 == <Keinen>
        if (currentUser != null) {
            for (int i = 0; i < list.size(); i++) {
                if (currentUser.getUsername().equals(list.get(i).getUsername())) {
                    idx = i;
                    break;
                }
            }
        }

        // Ring über [<Keinen>, user0..userN-1]
        int next = (idx + 1) % (list.size() + 1);
        UserRegistry.User nextUser = (next == 0) ? null : list.get(next - 1);

        setCurrentUser(nextUser);
        return nextUser;
    }
}
