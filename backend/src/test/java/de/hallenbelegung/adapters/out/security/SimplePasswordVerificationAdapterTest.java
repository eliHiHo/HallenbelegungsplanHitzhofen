package de.hallenbelegung.adapters.out.security;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplePasswordVerificationAdapterTest {

    private final SimplePasswordVerificationAdapter adapter = new SimplePasswordVerificationAdapter();

    @Test
    void matches_returns_false_for_null_inputs() {
        assertFalse(adapter.matches(null, "$2a$10$invalid"));
        assertFalse(adapter.matches("secret", null));
    }

    @Test
    void matches_returns_true_for_matching_password_and_hash() {
        String hash = BCrypt.hashpw("secret", BCrypt.gensalt());

        assertTrue(adapter.matches("secret", hash));
    }

    @Test
    void matches_returns_false_for_non_matching_or_invalid_hash() {
        String hash = BCrypt.hashpw("secret", BCrypt.gensalt());

        assertFalse(adapter.matches("wrong", hash));
        assertFalse(adapter.matches("secret", "not-a-bcrypt-hash"));
    }
}

