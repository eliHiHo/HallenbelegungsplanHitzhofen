package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBlockedTime;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BlockedTimePersistenceMapperTest {

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
    void maps_blocked_time_and_handles_null() {
        BlockedTimePersistenceMapper mapper = new BlockedTimePersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());
        assertNull(mapper.toDomain(null));
        assertNull(mapper.toEntity(null));

        DBUser user = new DBUser();
        user.setId(UUID.randomUUID());
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail("admin@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        setField(user, "createdAt", Instant.now());
        setField(user, "updatedAt", Instant.now());

        DBHall hall = new DBHall();
        hall.setId(UUID.randomUUID());
        hall.setName("Halle A");
        hall.setDescription("desc");
        hall.setActive(true);
        hall.setHallType(HallType.PART_SMALL);
        setField(hall, "createdAt", Instant.now());
        setField(hall, "updatedAt", Instant.now());

        DBBlockedTime entity = new DBBlockedTime();
        entity.setId(UUID.randomUUID());
        entity.setReason("Wartung");
        entity.setStartAt(LocalDateTime.of(2026, 5, 4, 8, 0));
        entity.setEndAt(LocalDateTime.of(2026, 5, 4, 10, 0));
        entity.setType(BlockedTimeType.MANUAL);
        entity.setHall(hall);
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        BlockedTime domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());

        DBBlockedTime mapped = mapper.toEntity(domain);
        assertEquals(domain.getReason(), mapped.getReason());
    }
}

