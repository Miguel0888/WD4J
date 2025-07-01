package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.util.WindowsCryptoUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserRegistry {

    private static final UserRegistry INSTANCE = new UserRegistry();
    private final List<User> users = new ArrayList<>();

    private UserRegistry() {
        load();
    }

    public static UserRegistry getInstance() {
        return INSTANCE;
    }

    public List<User> getAll() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

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

    public static class User {
        private String username;
        private String encryptedPassword;
        private String startPage;
        private String otpSecret;

        public User(String username, String encryptedPassword, String startPage, String otpSecret) {
            this.username = username;
            this.encryptedPassword = encryptedPassword;
            this.startPage = startPage;
            this.otpSecret = otpSecret;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDecryptedPassword() {
            try {
                return WindowsCryptoUtil.decrypt(encryptedPassword);
            } catch (Exception e) {
                return "";
            }
        }

        public void setEncryptedPassword(String encrypted) { this.encryptedPassword = encrypted; }

        public String getStartPage() { return startPage; }
        public void setStartPage(String startPage) { this.startPage = startPage; }

        public String getOtpSecret() { return otpSecret; }
        public void setOtpSecret(String otpSecret) { this.otpSecret = otpSecret; }

        @Override
        public String toString() {
            return username;
        }
    }
}
