package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBooking;
import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeries;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaBookingRepositoryTest {

    private EntityManager em;
    private JpaBookingRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaBookingRepository();
        repository.em = em;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DBUser dbUser(UUID id) {
        DBUser u = new DBUser();
        u.setId(id);
        u.setFirstName("Max");
        u.setLastName("Mustermann");
        u.setEmail("max@example.com");
        u.setPasswordHash("hash");
        u.setRole(Role.CLUB_REPRESENTATIVE);
        u.setActive(true);
        setField(u, "createdAt", Instant.now());
        setField(u, "updatedAt", Instant.now());
        return u;
    }

    private DBHall dbHall(UUID id) {
        DBHall h = new DBHall();
        h.setId(id);
        h.setName("Halle A");
        h.setDescription("desc");
        h.setHallType(HallType.PART_SMALL);
        h.setActive(true);
        setField(h, "createdAt", Instant.now());
        setField(h, "updatedAt", Instant.now());
        return h;
    }

    private DBBookingSeries dbSeries(UUID id, DBHall hall, DBUser user) {
        DBBookingSeries s = new DBBookingSeries();
        s.setId(id);
        s.setTitle("Serie");
        s.setDescription("desc");
        s.setWeekday(DayOfWeek.MONDAY);
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setStartDate(LocalDate.of(2026, 1, 1));
        s.setEndDate(LocalDate.of(2026, 12, 31));
        s.setStatus(BookingSeriesStatus.ACTIVE);
        s.setHall(hall);
        s.setResponsibleUser(user);
        s.setCreatedBy(user);
        s.setUpdatedBy(user);
        s.setCancelledBy(null);
        s.setCancelReason(null);
        s.setCancelledAt(null);
        setField(s, "createdAt", Instant.now());
        setField(s, "updatedAt", Instant.now());
        return s;
    }

    private DBBooking dbBooking(UUID id) {
        DBHall hall = dbHall(UUID.randomUUID());
        DBUser user = dbUser(UUID.randomUUID());

        DBBooking b = new DBBooking();
        b.setId(id);
        b.setTitle("Training");
        b.setDescription("desc");
        b.setStartAt(LocalDateTime.of(2026, 5, 4, 10, 0));
        b.setEndAt(LocalDateTime.of(2026, 5, 4, 11, 0));
        b.setStatus(BookingStatus.APPROVED);
        b.setParticipantCount(null);
        b.setConducted(false);
        b.setFeedbackComment(null);
        b.setCancelReason(null);
        b.setHall(hall);
        b.setResponsibleUser(user);
        b.setBookingSeries(dbSeries(UUID.randomUUID(), hall, user));
        b.setCreatedBy(user);
        b.setUpdatedBy(user);
        b.setCancelledBy(null);
        b.setCancelledAt(null);
        setField(b, "createdAt", Instant.now());
        setField(b, "updatedAt", Instant.now());
        return b;
    }

    @Test
    void findByTimeRange_binds_parameters_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBooking> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 4, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 4, 12, 0);

        when(em.createQuery(anyString(), eq(DBBooking.class))).thenReturn(query);
        when(query.setParameter("start", start)).thenReturn(query);
        when(query.setParameter("end", end)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbBooking(id)));

        List<Booking> result = repository.findByTimeRange(start, end);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void findByHallIdAndTimeRange_binds_parameters_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBooking> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 4, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 4, 12, 0);

        when(em.createQuery(anyString(), eq(DBBooking.class))).thenReturn(query);
        when(query.setParameter("hallId", hallId)).thenReturn(query);
        when(query.setParameter("start", start)).thenReturn(query);
        when(query.setParameter("end", end)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbBooking(id)));

        List<Booking> result = repository.findByHallIdAndTimeRange(hallId, start, end);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void findByResponsibleUserId_and_findByBookingSeriesId_bind_parameters() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBooking> query1 = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DBBooking> query2 = mock(TypedQuery.class);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();

        when(em.createQuery("SELECT b FROM DBBooking b WHERE b.responsibleUser.id = :userId", DBBooking.class)).thenReturn(query1);
        when(query1.setParameter("userId", userId)).thenReturn(query1);
        when(query1.getResultList()).thenReturn(List.of(dbBooking(id1)));

        when(em.createQuery("SELECT b FROM DBBooking b WHERE b.bookingSeries.id = :seriesId", DBBooking.class)).thenReturn(query2);
        when(query2.setParameter("seriesId", seriesId)).thenReturn(query2);
        when(query2.getResultList()).thenReturn(List.of(dbBooking(id2)));

        List<Booking> byUser = repository.findByResponsibleUserId(userId);
        List<Booking> bySeries = repository.findByBookingSeriesId(seriesId);

        assertEquals(id1, byUser.get(0).getId());
        assertEquals(id2, bySeries.get(0).getId());
    }
}

