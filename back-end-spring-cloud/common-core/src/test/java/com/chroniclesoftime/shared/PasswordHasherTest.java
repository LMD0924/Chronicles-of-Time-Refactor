package com.chroniclesoftime.shared;

import com.chroniclesoftime.auth.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void matchesOwnHash() {
        String h = PasswordHasher.hash("Cot@2026user");
        assertTrue(h.startsWith("pbkdf2$120000$"));
        assertTrue(PasswordHasher.matches("Cot@2026user", h));
    }

    @Test void rejectsWrongPassword() {
        String h = PasswordHasher.hash("correct-password");
        assertFalse(PasswordHasher.matches("wrong", h));
        assertFalse(PasswordHasher.matches(null, h));
        assertFalse(PasswordHasher.matches("x", "not-a-hash"));
    }

    @Test void saltedHashesDiffer() {
        assertNotEquals(PasswordHasher.hash("same"), PasswordHasher.hash("same"));
    }
}
