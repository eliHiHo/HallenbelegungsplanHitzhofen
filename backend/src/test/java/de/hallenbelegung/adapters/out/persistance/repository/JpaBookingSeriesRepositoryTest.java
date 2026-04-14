package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeries;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
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

class JpaBookingSeriesRepositoryTest {

    private EntityManager em;
    private JpaBookingSeriesRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaBookingSeriesRepository();
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

    private DBBookingSeries dbSeries(UUID id) {
        DBHall hall = dbHall(UUID.randomUUID());
        DBUser user = dbUser(UUID.randomUUID());

        DBBookingSeries s = new DBBookingSeries();
        s.setId(id);
        s.setTitle("Serie");
        s.setDescription("desc");
        s.setWeekday(DayOfWeek.MONDAY);
        s.setStartTime(LocalTime.of(18, 0));
        s.setEndTime(LocalTime.of(19, 0));
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

    @Test
    void save_and_findById_map_hit_and_miss() {
        UUID id = UUID.randomUUID();
        when(em.merge(any(DBBookingSeries.class))).thenReturn(dbSeries(id));

        BookingSeries saved = repository.save(BookingSeries.createNew(
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new de.hallenbelegung.application.domain.model.Hall(UUID.randomUUID(), "H", "d", true, Instant.now(), Instant.now(), HallType.PART_SMALL),
                new de.hallenbelegung.application.domain.model.User(UUID.randomUUID(), "A", "B", "a@b.c", "h", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now())
        ));
        assertEquals(id, saved.getId());

        when(em.find(DBBookingSeries.class, id)).thenReturn(dbSeries(id));
        Optional<BookingSeries> found = repository.findById(id);
        assertTrue(found.isPresent());

        when(em.find(DBBookingSeries.class, id)).thenReturn(null);
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void findByHallId_and_findByResponsibleUserId_bind_parameters() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeries> byHallQuery = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeries> byUserQuery = mock(TypedQuery.class);

        UUID hallId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(em.createQuery("SELECT s FROM DBBookingSeries s WHERE s.hall.id = :hallId", DBBookingSeries.class)).thenReturn(byHallQuery);
        when(byHallQuery.setParameter("hallId", hallId)).thenReturn(byHallQuery);
        when(byHallQuery.getResultList()).thenReturn(List.of(dbSeries(id1)));

        when(em.createQuery("SELECT s FROM DBBookingSeries s WHERE s.responsibleUser.id = :userId", DBBookingSeries.class)).thenReturn(byUserQuery);
        when(byUserQuery.setParameter("userId", userId)).thenReturn(byUserQuery);
        when(byUserQuery.getResultList()).thenReturn(List.of(dbSeries(id2)));

        assertEquals(id1, repository.findByHallId(hallId).get(0).getId());
        assertEquals(id2, repository.findByResponsibleUserId(userId).get(0).getId());
    }

    @Test
    void findActiveByHallIdAndDateRange_and_findAll_map_results() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeries> rangeQuery = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DBBookingSeries> allQuery = mock(TypedQuery.class);

        UUID hallId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(em.createQuery(anyString(), eq(DBBookingSeries.class))).thenReturn(rangeQuery, allQuery);

        when(rangeQuery.setParameter("hallId", hallId)).thenReturn(rangeQuery);
        when(rangeQuery.setParameter("startDate", from)).thenReturn(rangeQuery);
        when(rangeQuery.setParameter("endDate", to)).thenReturn(rangeQuery);
        when(rangeQuery.getResultList()).thenReturn(List.of(dbSeries(id1)));

        when(allQuery.getResultList()).thenReturn(List.of(dbSeries(id2)));

        List<BookingSeries> range = repository.findActiveByHallIdAndDateRange(hallId, from, to);
        List<BookingSeries> all = repository.findAll();

        assertEquals(id1, range.get(0).getId());
        assertEquals(id2, all.get(0).getId());
    }
}

