package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HallPersistenceMapperTest {

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
        HallPersistenceMapper mapper = new HallPersistenceMapper();
        assertNull(mapper.toDomain(null));
        assertNull(mapper.toEntity(null));

        DBHall entity = new DBHall();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setName("Halle A");
        entity.setDescription("desc");
        entity.setActive(true);
        entity.setHallType(HallType.FULL);
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        Hall domain = mapper.toDomain(entity);
        assertEquals(id, domain.getId());
        assertEquals(HallType.FULL, domain.getHallType());

        DBHall mapped = mapper.toEntity(domain);
        assertEquals("Halle A", mapped.getName());
    }
}

