package com.chroniclesoftime.shared;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void roundTripFlatObject() {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("uid", 12L); in.put("sub", "user"); in.put("role", "ADMIN"); in.put("ver", 3);
        Map<String, Object> out = Json.read(Json.write(in));
        assertEquals(12L, ((Number) out.get("uid")).longValue());
        assertEquals("user", out.get("sub"));
        assertEquals("ADMIN", out.get("role"));
    }

    @Test void escapesQuotes() {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("text", "a \"quote\" and \\ slash");
        assertEquals("a \"quote\" and \\ slash", Json.read(Json.write(in)).get("text"));
    }
}
