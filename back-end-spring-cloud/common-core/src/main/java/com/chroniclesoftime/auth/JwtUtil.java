package com.chroniclesoftime.auth;

import com.chroniclesoftime.shared.Json;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** 纯 JDK HS256 JWT，payload 只含 uid / sub / role / ver / iat / exp。 */
public class JwtUtil {
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private final byte[] secret;
    private final long ttlMillis;

    public JwtUtil(String secret, long ttlMillis) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = ttlMillis;
    }

    public String issue(Long uid, String username, String role, int version) {
        long now = System.currentTimeMillis();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256"); header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uid", uid); payload.put("sub", username); payload.put("role", role);
        payload.put("ver", version); payload.put("iat", now / 1000); payload.put("exp", (now + ttlMillis) / 1000);
        String h = B64.encodeToString(Json.write(header).getBytes(StandardCharsets.UTF_8));
        String p = B64.encodeToString(Json.write(payload).getBytes(StandardCharsets.UTF_8));
        return h + "." + p + "." + B64.encodeToString(hmac(h + "." + p));
    }

    public Map<String, Object> verify(String token) {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        String expected = B64.encodeToString(hmac(parts[0] + "." + parts[1]));
        if (!java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) return null;
        Map<String, Object> payload;
        try { payload = Json.read(new String(DEC.decode(parts[1]), StandardCharsets.UTF_8)); }
        catch (RuntimeException e) { return null; }
        Object exp = payload.get("exp");
        if (exp instanceof Number && ((Number) exp).longValue() * 1000 < System.currentTimeMillis()) return null;
        return payload;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("JWT 签名失败", e); }
    }
}
