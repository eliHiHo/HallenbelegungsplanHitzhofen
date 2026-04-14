package de.hallenbelegung.adapters.out.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Sha256TokenHashingAdapterTest {

    private final Sha256TokenHashingAdapter adapter = new Sha256TokenHashingAdapter();

    @Test
    void hash_returns_null_for_null_input() {
        assertNull(adapter.hash(null));
    }

    @Test
    void hash_is_deterministic_and_input_sensitive() {
        String hashA1 = adapter.hash("token-a");
        String hashA2 = adapter.hash("token-a");
        String hashB = adapter.hash("token-b");

        assertEquals(hashA1, hashA2);
        assertNotEquals(hashA1, hashB);
    }

    @Test
    void hash_matches_known_sha256_base64_value() {
        assertEquals("LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=", adapter.hash("hello"));
    }
}

