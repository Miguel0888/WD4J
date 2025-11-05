package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.config.LoginConfig;
import de.bund.zrb.util.WindowsCryptoUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UserRegistry {

    private static final UserRegistry INSTANCE = new UserRegistry();
    private final List<User> users = new ArrayList<>();

    private UserRegistry() { load(); }

    public static UserRegistry getInstance() { return INSTANCE; }

    public List<User> getAll() { return users; }

    public void addUser(User user) { users.add(user); }

    public void removeUser(User user) { users.remove(user); }

    public void save() {
        SettingsService.getInstance().save("users.json", users);
    }

    public void load() {
        Type type = new TypeToken<List<User>>() {}.getType();
        List<User> loaded = SettingsService.getInstance().load("users.json", type);
        if (loaded != null) {
            users.clear();
            users.addAll(loaded);
        }
    }

    public User getUser(String userId) {
        for (User user : users) {
            if (userId.equals(user.getUsername())) return user;
        }
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public static class User {
        private String username;
        private String encryptedPassword;
        private String startPage;
        private String otpSecret;

        /** LoginConfig ist jetzt eine eigenständige Klasse (de.bund.zrb.config) */
        private LoginConfig loginConfig;

        public User(String username, String encryptedPassword) {
            this.username = username;
            this.encryptedPassword = encryptedPassword;
        }

        public User(String username, String encryptedPassword, String startPage, String otpSecret, LoginConfig loginConfig) {
            this.username = username;
            this.encryptedPassword = encryptedPassword;
            this.startPage = startPage;
            this.otpSecret = otpSecret;
            this.loginConfig = loginConfig;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDecryptedPassword() {
            try { return WindowsCryptoUtil.decrypt(encryptedPassword); }
            catch (Exception e) { return ""; }
        }
        public void setEncryptedPassword(String encrypted) { this.encryptedPassword = encrypted; }

        public String getStartPage() { return startPage; }
        public void setStartPage(String startPage) { this.startPage = startPage; }

        public String getOtpSecret() { return otpSecret; }
        public void setOtpSecret(String otpSecret) { this.otpSecret = otpSecret; }

        public LoginConfig getLoginConfig() {
            if (loginConfig == null) loginConfig = new LoginConfig();
            return loginConfig;
        }
        public void setLoginConfig(LoginConfig config) { this.loginConfig = config; }

        @Override
        public String toString() { return username; }
    }

    /**
     * Return current usernames as a materialized list.
     * Keep this small and reusable for other methods.
     */
    public List<String> getUsernames() {
        return getAll()
                .stream()
                .map(UserRegistry.User::getUsername)
                .collect(Collectors.toList());
    }

    /**
     * Provide a live supplier that reads current usernames on every get().
     * Prefer this when registry content may change and freshness matters.
     */
    public Supplier<List<String>> usernamesSupplier() {
        return new Supplier<List<String>>() {
            @Override
            public List<String> get() {
                // Read fresh usernames each time
                return getUsernames();
            }
        };
    }

    /**
     * Provide a snapshot supplier that captures usernames once.
     * Prefer this when stability and performance are more important than freshness.
     */
    public Supplier<List<String>> usernamesSnapshotSupplier() {
        final List<String> snapshot = getUsernames();
        return new Supplier<List<String>>() {
            @Override
            public List<String> get() {
                // Return captured snapshot
                return snapshot;
            }
        };
    }
}
