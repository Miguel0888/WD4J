package de.bund.zrb.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class TotpService {

    private static final TotpService INSTANCE = new TotpService();

    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();

    private TotpService() {
        // private constructor for singleton
    }

    public static TotpService getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a new secret key for TOTP setup (to be stored per user).
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Generates the current OTP for the given secret.
     */
    public int generateCurrentOtp(String secret) {
        return authenticator.getTotpPassword(secret);
    }

    /**
     * Validates a user-supplied OTP code against the current time window.
     */
    public boolean isOtpValid(String secret, int code) {
        return authenticator.authorize(secret, code);
    }

    public String getOtp() {
        String username = UserContextMappingService.getInstance().getCurrentUsernameOrNull();
        return getOtp(username);
    }

    public String getOtp(String username) {
        UserRegistry.User user = UserRegistry.getInstance().getUser(username);
        String otpSecret = user.getOtpSecret();
        return String.valueOf(generateCurrentOtp(otpSecret));
    }
}
