package de.bund.zrb.util;

import com.sun.jna.platform.win32.Crypt32;
import com.sun.jna.platform.win32.WinCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Required to securely save passwords on Windows machines
 * Only Windows User can encrypt once the password is saved!
 */
public class WindowsCryptoUtil {

    public static String encrypt(String plainText) {
        byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
        WinCrypt.DATA_BLOB dataIn = new WinCrypt.DATA_BLOB(data);
        WinCrypt.DATA_BLOB dataOut = new WinCrypt.DATA_BLOB();

        boolean success = Crypt32.INSTANCE.CryptProtectData(dataIn, null, null, null, null, 0, dataOut);
        if (!success) throw new IllegalStateException("Encryption failed");

        return Base64.getEncoder().encodeToString(dataOut.getData());
    }

    public static String decrypt(String base64) {
        byte[] encrypted = Base64.getDecoder().decode(base64);
        WinCrypt.DATA_BLOB dataIn = new WinCrypt.DATA_BLOB(encrypted);
        WinCrypt.DATA_BLOB dataOut = new WinCrypt.DATA_BLOB();

        boolean success = Crypt32.INSTANCE.CryptUnprotectData(dataIn, null, null, null, null, 0, dataOut);
        if (!success) throw new IllegalStateException("Decryption failed");

        return new String(dataOut.getData(), StandardCharsets.UTF_8);
    }
}
