package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingRequest;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaBookingRequestRepositoryTest {

    private EntityManager em;
    private JpaBookingRequestRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaBookingRequestRepository();
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

    private DBBookingRequest dbRequest(UUID id) {
        DBBookingRequest r = new DBBookingRequest();
        r.setId(id);
        r.setTitle("Anfrage");
        r.setDescription("desc");
        r.setStartAt(LocalDateTime.of(2026, 5, 4, 10, 0));
        r.setEndAt(LocalDateTime.of(2026, 5, 4, 11, 0));
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
    void findById_returns_optional_hit_and_miss() {
        UUID id = UUID.randomUUID();
        when(em.find(DBBookingRequest.class, id)).thenReturn(dbRequest(id));

        Optional<BookingRequest> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());

        when(em.find(DBBookingRequest.class, id)).thenReturn(null);
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void findByStatus_binds_status_parameter_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingRequest> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();

        when(em.createQuery(anyString(), eq(DBBookingRequest.class))).thenReturn(query);
        when(query.setParameter("status", BookingRequestStatus.PENDING)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbRequest(id)));

        List<BookingRequest> result = repository.findByStatus(BookingRequestStatus.PENDING);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void findByRequestedByUserId_binds_user_parameter_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingRequest> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(em.createQuery(anyString(), eq(DBBookingRequest.class))).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbRequest(id)));

        List<BookingRequest> result = repository.findByRequestedByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void findAll_maps_list() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingRequest> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();

        when(em.createQuery("SELECT r FROM DBBookingRequest r", DBBookingRequest.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbRequest(id)));

        List<BookingRequest> result = repository.findAll();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }
}

