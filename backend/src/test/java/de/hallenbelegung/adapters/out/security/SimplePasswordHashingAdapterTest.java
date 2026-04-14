package de.hallenbelegung.adapters.out.security;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplePasswordHashingAdapterTest {

    private final SimplePasswordHashingAdapter adapter = new SimplePasswordHashingAdapter();

    @Test
    void hash_returns_null_for_null_input() {
        assertNull(adapter.hash(null));
    }

    @Test
    void hash_returns_bcrypt_hash_that_matches_raw_password() {
        String raw = "Sup3rSecret!";

        String hash = adapter.hash(raw);

        assertNotNull(hash);
        assertNotEquals(raw, hash);
        assertTrue(BCrypt.checkpw(raw, hash));
    }
}

