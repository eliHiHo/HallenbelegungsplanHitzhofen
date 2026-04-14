package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBooking;
import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeries;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingPersistenceMapperTest {

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
    void maps_booking_and_handles_null_and_conducted_null() {
        BookingPersistenceMapper mapper = new BookingPersistenceMapper(
                new HallPersistenceMapper(),
                new UserPersistenceMapper(),
                new BookingSeriesPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper())
        );
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

        DBBookingSeries series = new DBBookingSeries();
        series.setId(UUID.randomUUID());
        series.setTitle("Serie");
        series.setDescription("desc");
        series.setWeekday(DayOfWeek.MONDAY);
        series.setStartTime(LocalTime.of(18, 0));
        series.setEndTime(LocalTime.of(19, 0));
        series.setStartDate(LocalDate.of(2026, 5, 1));
        series.setEndDate(LocalDate.of(2026, 6, 1));
        series.setStatus(BookingSeriesStatus.ACTIVE);
        series.setHall(hall);
        series.setResponsibleUser(user);
        series.setCreatedBy(user);
        series.setUpdatedBy(user);
        setField(series, "createdAt", Instant.now());
        setField(series, "updatedAt", Instant.now());

        DBBooking entity = new DBBooking();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Training");
        entity.setDescription("desc");
        entity.setStartAt(LocalDateTime.of(2026, 5, 4, 10, 0));
        entity.setEndAt(LocalDateTime.of(2026, 5, 4, 11, 0));
        entity.setStatus(BookingStatus.APPROVED);
        entity.setParticipantCount(12);
        entity.setConducted(null);
        entity.setFeedbackComment("ok");
        entity.setCancelReason("Grund");
        entity.setHall(hall);
        entity.setResponsibleUser(user);
        entity.setBookingSeries(series);
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setCancelledBy(user);
        entity.setCancelledAt(Instant.now());
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());

        Booking domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());
        assertFalse(domain.isConducted());

        DBBooking mapped = mapper.toEntity(domain);
        assertEquals(domain.getCancelReason(), mapped.getCancelReason());
        assertEquals(domain.getCancelledAt(), mapped.getCancelledAt());
    }
}

