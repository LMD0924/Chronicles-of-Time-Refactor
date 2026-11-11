package com.chroniclesoftime.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2WithHmacSHA256，格式 pbkdf2$iterations$salt-b64url$hash-b64url。 */
public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private PasswordHasher() {}

    public static String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + B64.encodeToString(salt) + "$" + B64.encodeToString(derive(raw, salt, ITERATIONS));
    }

    public static boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null) return false;
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = DEC.decode(parts[2]);
            byte[] expected = DEC.decode(parts[3]);
            byte[] actual = derive(raw, salt, iterations);
            return java.security.MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) { return false; }
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) { throw new IllegalStateException("密码派生失败", e); }
    }
}
