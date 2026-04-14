package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingRequest;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingRequestPersistenceMapperTest {

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
    void maps_booking_request_and_handles_null() {
        BookingRequestPersistenceMapper mapper = new BookingRequestPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());
        assertNull(mapper.toDomain(null));
        assertNull(mapper.toEntity(null));

        DBUser user = new DBUser();
        user.setId(UUID.randomUUID());
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        user.setEmail("max@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.CLUB_REPRESENTATIVE);
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

        DBBookingRequest entity = new DBBookingRequest();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Anfrage");
        entity.setDescription("desc");
        entity.setStartAt(LocalDateTime.of(2026, 5, 4, 10, 0));
        entity.setEndAt(LocalDateTime.of(2026, 5, 4, 11, 0));
        entity.setStatus(BookingRequestStatus.PENDING);
        entity.setHall(hall);
        entity.setRequestedBy(user);
        entity.setProcessedBy(user);
        entity.setRejectionReason("none");
        entity.setProcessedAt(Instant.now());
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        BookingRequest domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());
        assertEquals("Anfrage", domain.getTitle());

        DBBookingRequest mapped = mapper.toEntity(domain);
        assertEquals(domain.getId(), mapped.getId());
    }
}

