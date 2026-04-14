package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeriesRequest;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingSeriesRequestPersistenceMapperTest {

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
    void maps_booking_series_request_and_handles_null() {
        BookingSeriesRequestPersistenceMapper mapper = new BookingSeriesRequestPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());
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

        DBBookingSeriesRequest entity = new DBBookingSeriesRequest();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Serie Anfrage");
        entity.setDescription("desc");
        entity.setWeekday(DayOfWeek.MONDAY);
        entity.setStartTime(LocalTime.of(18, 0));
        entity.setEndTime(LocalTime.of(19, 0));
        entity.setStartDate(LocalDate.of(2026, 5, 1));
        entity.setEndDate(LocalDate.of(2026, 6, 1));
        entity.setStatus(BookingRequestStatus.PENDING);
        entity.setHall(hall);
        entity.setRequestedBy(user);
        entity.setProcessedBy(user);
        entity.setProcessedAt(Instant.now());
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        BookingSeriesRequest domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());

        DBBookingSeriesRequest mapped = mapper.toEntity(domain);
        assertEquals(domain.getStartDate(), mapped.getStartDate());
        assertEquals(domain.getEndDate(), mapped.getEndDate());
    }
}

