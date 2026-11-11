package com.chroniclesoftime.shared;

import com.chroniclesoftime.auth.JwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private final JwtUtil jwt = new JwtUtil("unit-test-secret", 3600_000L);

    @Test void issueAndVerify() {
        String token = jwt.issue(7L, "user", "USER", 2);
        Map<String, Object> payload = jwt.verify(token);
        assertNotNull(payload);
        assertEquals(7L, ((Number) payload.get("uid")).longValue());
        assertEquals("user", payload.get("sub"));
        assertEquals("USER", payload.get("role"));
        assertEquals(2L, ((Number) payload.get("ver")).longValue());
    }

    @Test void rejectsTamperedAndExpired() {
        String token = jwt.issue(1L, "user", "USER", 1);
        assertNull(jwt.verify(token + "x"));
        assertNull(jwt.verify(null));
        JwtUtil expired = new JwtUtil("unit-test-secret", -1000L);
        assertNull(expired.verify(expired.issue(1L, "user", "USER", 1)));
    }
}
