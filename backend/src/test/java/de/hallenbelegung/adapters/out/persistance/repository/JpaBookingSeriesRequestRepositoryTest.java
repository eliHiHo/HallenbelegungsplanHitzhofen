package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeriesRequest;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaBookingSeriesRequestRepositoryTest {

    private EntityManager em;
    private JpaBookingSeriesRequestRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaBookingSeriesRequestRepository();
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

    private DBBookingSeriesRequest dbRequest(UUID id) {
        DBBookingSeriesRequest r = new DBBookingSeriesRequest();
        r.setId(id);
        r.setTitle("Serie Anfrage");
        r.setDescription("desc");
        r.setWeekday(DayOfWeek.MONDAY);
        r.setStartTime(LocalTime.of(18, 0));
        r.setEndTime(LocalTime.of(19, 0));
        r.setStartDate(LocalDate.of(2026, 5, 1));
        r.setEndDate(LocalDate.of(2026, 6, 1));
        r.setStatus(BookingRequestStatus.PENDING);
        r.setHall(dbHall(UUID.randomUUID()));
        r.setRequestedBy(dbUser(UUID.randomUUID()));
        r.setProcessedBy(null);
        r.setRejectionReason(null);
        r.setProcessedAt(null);
        setField(r, "createdAt", Instant.now());
        setField(r, "updatedAt", Instant.now());
        return r;
    }

    @Test
    void save_and_findById_hit_and_miss() {
        UUID id = UUID.randomUUID();
        when(em.merge(any(DBBookingSeriesRequest.class))).thenReturn(dbRequest(id));

        BookingSeriesRequest saved = repository.save(new BookingSeriesRequest(
                null,
                "Serie Anfrage",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingRequestStatus.PENDING,
                null,
                new de.hallenbelegung.application.domain.model.Hall(UUID.randomUUID(), "H", "d", true, Instant.now(), Instant.now(), HallType.PART_SMALL),
                new de.hallenbelegung.application.domain.model.User(UUID.randomUUID(), "A", "B", "a@b.c", "h", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now()),
                null,
                Instant.now(),
                Instant.now(),
                null
        ));
        assertEquals(id, saved.getId());

        when(em.find(DBBookingSeriesRequest.class, id)).thenReturn(dbRequest(id));
        Optional<BookingSeriesRequest> found = repository.findById(id);
        assertTrue(found.isPresent());

        when(em.find(DBBookingSeriesRequest.class, id)).thenReturn(null);
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void findByStatus_and_findByRequestedByUserId_bind_parameters() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeriesRequest> byStatusQuery = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeriesRequest> byUserQuery = mock(TypedQuery.class);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(em.createQuery("SELECT r FROM DBBookingSeriesRequest r WHERE r.status = :status", DBBookingSeriesRequest.class)).thenReturn(byStatusQuery);
        when(byStatusQuery.setParameter("status", BookingRequestStatus.PENDING)).thenReturn(byStatusQuery);
        when(byStatusQuery.getResultList()).thenReturn(List.of(dbRequest(id1)));

        when(em.createQuery("SELECT r FROM DBBookingSeriesRequest r WHERE r.requestedBy.id = :userId", DBBookingSeriesRequest.class)).thenReturn(byUserQuery);
        when(byUserQuery.setParameter("userId", userId)).thenReturn(byUserQuery);
        when(byUserQuery.getResultList()).thenReturn(List.of(dbRequest(id2)));

        List<BookingSeriesRequest> byStatus = repository.findByStatus(BookingRequestStatus.PENDING);
        List<BookingSeriesRequest> byUser = repository.findByRequestedByUserId(userId);

        assertEquals(id1, byStatus.get(0).getId());
        assertEquals(id2, byUser.get(0).getId());
    }

    @Test
    void findAll_maps_list() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeriesRequest> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();

        when(em.createQuery("SELECT r FROM DBBookingSeriesRequest r", DBBookingSeriesRequest.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbRequest(id)));

        List<BookingSeriesRequest> result = repository.findAll();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }
}

