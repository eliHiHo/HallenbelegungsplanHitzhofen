package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserPersistenceMapperTest {

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void maps_domain_and_entity_and_handles_null() {
        UserPersistenceMapper mapper = new UserPersistenceMapper();
        assertNull(mapper.toDomain(null));
        assertNull(mapper.toEntity(null));

        DBUser entity = new DBUser();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setFirstName("Max");
        entity.setLastName("Mustermann");
        entity.setEmail("max@example.com");
        entity.setPasswordHash("hash");
        entity.setRole(Role.ADMIN);
        entity.setActive(true);
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        User domain = mapper.toDomain(entity);
        assertEquals(id, domain.getId());

        DBUser mappedBack = mapper.toEntity(domain);
        assertNotNull(mappedBack);
        assertEquals("max@example.com", mappedBack.getEmail());
    }
}

