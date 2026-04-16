package de.hallenbelegung.adapters.out.bootstrap;

import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates realistic development seed data on startup if the database is empty.
 *
 * Guard: only runs when {@code app.seed.enabled=true} AND no bookings exist
 * (checked via a 10-year time window). This prevents any accidental duplicate
 * seeding on subsequent restarts when data already exists.
 *
 * Enabled automatically in dev profile via {@code %dev.app.seed.enabled=true}
 * in application.properties.
 */
@ApplicationScoped
public class SeedDataInitializer {

    private static final Logger LOG = Logger.getLogger(SeedDataInitializer.class.getName());
    private static final String CLUB_SEED_PASSWORD = "Test1234";

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    HallRepositoryPort hallRepository;

    @Inject
    BookingRepositoryPort bookingRepository;

    @Inject
    BookingSeriesRepositoryPort bookingSeriesRepository;

    @Inject
    BlockedTimeRepositoryPort blockedTimeRepository;

    @Inject
    PasswordHashingPort passwordHashingPort;

    @ConfigProperty(name = "app.seed.enabled", defaultValue = "false")
    boolean seedEnabled;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!seedEnabled) {
            return;
        }

        // Guard: skip if bookings already exist (covers ±5 years from today)
        LocalDateTime windowStart = LocalDateTime.now().minusYears(5);
        LocalDateTime windowEnd   = LocalDateTime.now().plusYears(5);
        if (!bookingRepository.findByTimeRange(windowStart, windowEnd).isEmpty()) {
            LOG.info("SeedDataInitializer: bookings already present — skipping seed.");
            return;
        }

        LOG.info("SeedDataInitializer: empty database detected, creating seed data...");

        List<Hall> halls     = ensureHalls();
        List<User> clubUsers = createClubUsers();

        if (halls.isEmpty() || clubUsers.isEmpty()) {
            LOG.warning("SeedDataInitializer: no halls or club users available — aborting seed.");
            return;
        }

        createSeedBookings(halls, clubUsers);
        createSeedBookingSeries(halls, clubUsers);
        createSeedBlockedTime(halls);

        LOG.info("SeedDataInitializer: seed data created successfully.");
    }

    // -------------------------------------------------------------------------
    // Halls
    // -------------------------------------------------------------------------

    /**
     * Returns the active halls already in the DB, or creates three seed halls
     * if the hall table is empty (fresh database without import.sql).
     */
    private List<Hall> ensureHalls() {
        List<Hall> existing = hallRepository.findAllActive();
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Hall> created = new ArrayList<>();
        created.add(hallRepository.save(Hall.createNew("Turnhalle A",    "Kleiner Hallenteil",    HallType.PART_SMALL)));
        created.add(hallRepository.save(Hall.createNew("Turnhalle B",    "Großer Hallenteil",     HallType.PART_LARGE)));
        created.add(hallRepository.save(Hall.createNew("Sporthalle Groß","Vollständige Sporthalle",HallType.FULL)));
        return created;
    }

    // -------------------------------------------------------------------------
    // Club users
    // -------------------------------------------------------------------------

    private List<User> createClubUsers() {
        String hash = passwordHashingPort.hash(CLUB_SEED_PASSWORD);

        record SeedUser(String firstName, String lastName, String email) {}

        List<SeedUser> defs = List.of(
            new SeedUser("Erika",  "Mustermann", "verein1@test.de"),
            new SeedUser("Thomas", "Müller",     "verein2@test.de"),
            new SeedUser("Sabine", "Koch",        "verein3@test.de")
        );

        List<User> result = new ArrayList<>();
        for (SeedUser def : defs) {
            User user = userRepository.findByEmail(def.email())
                .orElseGet(() -> userRepository.save(
                    User.createNew(def.firstName(), def.lastName(), def.email(), hash, Role.CLUB_REPRESENTATIVE)
                ));
            result.add(user);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Standalone bookings spread across the current calendar week
    // -------------------------------------------------------------------------

    private void createSeedBookings(List<Hall> halls, List<User> users) {
        // Anchor to Monday of the current ISO week
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);

        record Spec(int dayOffset, int startHour, int endHour, String title, int hallIdx, int userIdx) {}

        List<Spec> specs = List.of(
            new Spec(0, 9,  11, "Volleyball Training",  0, 0),  // Mon
            new Spec(1, 14, 16, "Fußball Training",     1, 1),  // Tue
            new Spec(2, 18, 20, "Basketball Training",  2, 2),  // Wed
            new Spec(3, 10, 12, "Yoga Kurs",            0, 0),  // Thu
            new Spec(4, 16, 18, "Badminton Training",   1, 1)   // Fri
        );

        for (Spec s : specs) {
            LocalDate date  = monday.plusDays(s.dayOffset());
            LocalDateTime start = date.atTime(s.startHour(), 0);
            LocalDateTime end   = date.atTime(s.endHour(),   0);
            Hall hall = halls.get(s.hallIdx() % halls.size());
            User user = users.get(s.userIdx() % users.size());

            // Full overload: responsibleUser also acts as createdBy (seed context)
            Booking booking = Booking.createNew(
                s.title(),
                "Wöchentliches Training",
                start,
                end,
                hall,
                user,
                null,   // no series
                user,   // createdBy — DB column is NOT NULL
                null,
                null,
                null,
                null
            );
            bookingRepository.save(booking);
        }
    }

    // -------------------------------------------------------------------------
    // Recurring booking series — Tuesday evenings for ~3 months
    // -------------------------------------------------------------------------

    private void createSeedBookingSeries(List<Hall> halls, List<User> users) {
        Hall hall = halls.get(0);
        User user = users.get(0);

        // Series spans from 2 weeks ago (first Tuesday) to ~3 months ahead
        LocalDate seriesStart = LocalDate.now()
            .minusWeeks(2)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));
        LocalDate seriesEnd = LocalDate.now()
            .plusMonths(3)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));

        BookingSeries series = BookingSeries.createNew(
            "Dienstags Training Verein 1",
            "Wöchentliches Vereinstraining",
            DayOfWeek.TUESDAY,
            LocalTime.of(18, 0),
            LocalTime.of(20, 0),
            seriesStart,
            seriesEnd,
            hall,
            user
        );
        BookingSeries savedSeries = bookingSeriesRepository.save(series);

        // Create individual booking occurrence for every Tuesday in the range
        LocalDate cursor = seriesStart;
        while (!cursor.isAfter(seriesEnd)) {
            LocalDateTime start = cursor.atTime(18, 0);
            LocalDateTime end   = cursor.atTime(20, 0);

            Booking occurrence = Booking.createNew(
                "Dienstags Training",
                "Wöchentliches Vereinstraining",
                start,
                end,
                hall,
                user,
                savedSeries,
                user,   // createdBy — DB column is NOT NULL
                null,
                null,
                null,
                null
            );
            bookingRepository.save(occurrence);
            cursor = cursor.plusWeeks(1);
        }

        // Second series: Thursday morning yoga for Verein 2
        if (users.size() > 1 && halls.size() > 1) {
            User user2 = users.get(1);
            Hall hall2 = halls.get(halls.size() > 1 ? 1 : 0);

            LocalDate s2Start = LocalDate.now()
                .minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
            LocalDate s2End = LocalDate.now()
                .plusMonths(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));

            BookingSeries series2 = BookingSeries.createNew(
                "Donnerstags Sport Verein 2",
                "Donnerstägliches Training",
                DayOfWeek.THURSDAY,
                LocalTime.of(17, 0),
                LocalTime.of(19, 0),
                s2Start,
                s2End,
                hall2,
                user2
            );
            BookingSeries savedSeries2 = bookingSeriesRepository.save(series2);

            LocalDate cur2 = s2Start;
            while (!cur2.isAfter(s2End)) {
                Booking occ2 = Booking.createNew(
                    "Donnerstags Sport",
                    "Donnerstägliches Training",
                    cur2.atTime(17, 0),
                    cur2.atTime(19, 0),
                    hall2,
                    user2,
                    savedSeries2,
                    user2,
                    null, null, null, null
                );
                bookingRepository.save(occ2);
                cur2 = cur2.plusWeeks(1);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Blocked time — maintenance next Monday morning
    // -------------------------------------------------------------------------

    private void createSeedBlockedTime(List<Hall> halls) {
        User admin = userRepository.findAllActive()
            .stream()
            .filter(User::isAdmin)
            .findFirst()
            .orElse(null);

        if (admin == null) {
            LOG.warning("SeedDataInitializer: no admin user found — skipping blocked time.");
            return;
        }

        Hall hall = halls.get(0);
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDateTime start = nextMonday.atTime(8, 0);
        LocalDateTime end   = nextMonday.atTime(12, 0);

        BlockedTime blocked = BlockedTime.createNew(
            "Hallenwartung",
            start,
            end,
            BlockedTimeType.MANUAL,
            hall,
            admin
        );
        blockedTimeRepository.save(blocked);
    }
}
