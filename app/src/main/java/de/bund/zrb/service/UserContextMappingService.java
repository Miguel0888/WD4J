package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserContextMappingService {

    private static final UserContextMappingService INSTANCE = new UserContextMappingService();

    // Username → Context
    private final Map<String, BrowserContext> userContextMap = new HashMap<>();

    // Username → User
    private final Map<String, UserRegistry.User> userMap = new HashMap<>();

    // Username → UserContextId (persistiert)
    private final Map<String, String> userContextIdMap = new HashMap<>();

    // Property-Change für UI/Listener (z.B. StatusBar)
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

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
        // Context-IDs aus UserRegistry laden
        for (UserRegistry.User u : UserRegistry.getInstance().getAll()) {
            if (u.getLastUserContext() != null && !u.getLastUserContext().isEmpty()) {
                userContextIdMap.put(u.getUsername(), u.getLastUserContext());
            }
        }
    }

    public static UserContextMappingService getInstance() {
        return INSTANCE;
    }

    public void bindUserToContext(String username, BrowserContext context, UserRegistry.User user) {
        userContextMap.put(username, context);
        userMap.put(username, user);
        // Kontext-ID erfassen wenn verfügbar (unser UserContextImpl liefert ID)
        try {
            if (context instanceof de.bund.zrb.UserContextImpl) {
                String id = ((de.bund.zrb.UserContextImpl) context).getUserContext().value();
                if (id != null && !id.isEmpty()) {
                    setContextId(username, id); // persistiert
                }
            }
        } catch (Throwable ignore) {}
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
        // auch gespeicherte ID entfernen
        setContextId(username, null);
    }

    public synchronized UserRegistry.User getCurrentUser() {
        return currentUser;
    }

    /** Setzt den aktuellen Benutzer und persistiert ihn als defaultUser (null entfernt den Eintrag). */
    public synchronized void setCurrentUser(UserRegistry.User user) {
        UserRegistry.User old = this.currentUser;
        this.currentUser = user;

        if (user == null) {
            SettingsService.getInstance().set("defaultUser", null);
        } else {
            SettingsService.getInstance().set("defaultUser", user.getUsername());
        }

        // UI/Listener informieren
        pcs.firePropertyChange("currentUser", old, user);
    }

    public void setCurrentUserByContextId(String browsingContextId) {
        UserRegistry.User u = BrowserServiceImpl.getInstance().userForBrowsingContextId(browsingContextId);
        setCurrentUser(u);
    }

    /** Komfort-Helfer: liefert aktuellen Usernamen oder null. */
    public synchronized String getCurrentUsernameOrNull() {
        return (currentUser == null) ? null : currentUser.getUsername();
    }

    /** Speichert/entfernt die UserContext-ID für einen Benutzer und persistiert die Map. */
    public synchronized void setContextId(String username, String contextId) {
        if (username == null || username.trim().isEmpty()) return;
        if (contextId == null || contextId.trim().isEmpty()) {
            userContextIdMap.remove(username);
            UserRegistry.User u = UserRegistry.getInstance().getUser(username);
            if (u != null) { u.setLastUserContext(null); UserRegistry.getInstance().save(); }
        } else {
            userContextIdMap.put(username, contextId);
            UserRegistry.User u = UserRegistry.getInstance().getUser(username);
            if (u != null) { u.setLastUserContext(contextId); UserRegistry.getInstance().save(); }
        }
    }

    public synchronized String getContextId(String username) {
        return userContextIdMap.get(username);
    }

    public synchronized Map<String,String> getAllContextIds() {
        return new HashMap<String,String>(userContextIdMap);
    }

    /**
     * Zyklisches Weiterschalten in der Reihenfolge der UserRegistry-Liste.
     * Zyklus: <Keinen> → user0 → user1 → … → <Keinen>.
     * @return der neue aktuelle Benutzer, oder null für <Keinen>
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
        int next = (idx + 1) % (list.size());
        UserRegistry.User nextUser =  list.get(next);

        setCurrentUser(nextUser); // feuert Event & persistiert
        return nextUser;
    }

    // ---- Listener-API für UI-Schichten (Statusbar etc.) ----

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (l != null) pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (l != null) pcs.removePropertyChangeListener(l);
    }
}
