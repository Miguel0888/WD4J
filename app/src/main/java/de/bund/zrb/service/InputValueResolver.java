package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve template placeholders like {{OTP}} or {{Belegnummer}} to concrete runtime values.
 * Generate OTP at the last possible moment to avoid expiry.
 * Allow multiple placeholders in one string.
 */
public final class InputValueResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    private InputValueResolver() {
        // Hide constructor
    }

    /**
     * Resolve all placeholders in the action's value template.
     * Example:
     *   Template: "Login {{OTP}} / Beleg {{Belegnummer}}"
     *   Result:   "Login 123456 / Beleg 4711"
     *
     * Return empty string if template is null.
     */
    public static String resolveDynamicText(TestAction action) {
        String template = action.getValue();
        if (template == null) {
            return "";
        }

        Matcher m = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String token = m.group(1);
            String replacement = resolveSingleToken(token, action.getUser());

            // Escape backslashes and dollars for appendReplacement
            if (replacement == null) {
                replacement = "";
            }
            replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");

            m.appendReplacement(sb, replacement);
        }

        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolve a single token name without braces.
     * Token "OTP" is special. Everything else is treated as parameter name.
     */
    private static String resolveSingleToken(String tokenRaw, String userId) {
        if (tokenRaw == null) {
            return "";
        }

        String token = tokenRaw.trim();

        // Handle OTP
        if ("OTP".equalsIgnoreCase(token)) {
            return resolveOtpForUser(userId);
        }

        // Handle arbitrary parameter like {{Belegnummer}}
        String paramVal = ParameterRegistry.getInstance().getValue(token);
        return (paramVal != null) ? paramVal : "";
    }

    /**
     * Generate time-based OTP for a given logical test user.
     * Return "######" if no OTP secret is known.
     */
    private static String resolveOtpForUser(String userId) {
        if (userId == null) {
            return "######";
        }

        UserRegistry.User u = UserRegistry.getInstance().getUser(userId);
        if (u == null || u.getOtpSecret() == null) {
            return "######";
        }

        int code = TotpService.getInstance().generateCurrentOtp(u.getOtpSecret());
        return String.format("%06d", code);
    }
}
