package de.hallenbelegung.adapters.out.notification;

import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuarkusNotificationAdapterTest {

    private Mailer mailer;
    private UserRepositoryPort userRepository;
    private QuarkusNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        mailer = mock(Mailer.class);
        userRepository = mock(UserRepositoryPort.class);
        adapter = new QuarkusNotificationAdapter(mailer, userRepository);
    }

    private User user(Role role, String email) {
        return new User(UUID.randomUUID(), "Max", "Mustermann", email, "hash", role, true, Instant.now(), Instant.now());
    }

    private Hall hall(String name) {
        return new Hall(UUID.randomUUID(), name, "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
    }

    private BookingRequest bookingRequest(User requester, Hall hall) {
        return new BookingRequest(
                UUID.randomUUID(),
                "Anfrage",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private Booking booking(User requester, Hall hall) {
        return new Booking(
                UUID.randomUUID(),
                "Buchung",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                BookingStatus.APPROVED,
                null,
                false,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                requester,
                null,
                null,
                null,
                null
        );
    }

    private BookingSeriesRequest seriesRequest(User requester, Hall hall) {
        return new BookingSeriesRequest(
                UUID.randomUUID(),
                "Serie Anfrage",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private BookingSeries series(User requester, Hall hall) {
        return new BookingSeries(
                UUID.randomUUID(),
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingSeriesStatus.ACTIVE,
                hall,
                requester,
                requester,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }

    @Test
    void notifyAdminsAboutNewBookingRequest_sends_only_to_active_admins() {
        User admin = user(Role.ADMIN, "admin@example.com");
        User rep = user(Role.CLUB_REPRESENTATIVE, "rep@example.com");
        when(userRepository.findAllActive()).thenReturn(List.of(admin, rep));

        adapter.notifyAdminsAboutNewBookingRequest(bookingRequest(rep, hall("Halle A")));

        ArgumentCaptor<Mail[]> captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer).send(captor.capture());
        Mail sent = captor.getValue()[0];
        assertEquals("admin@example.com", sent.getTo().get(0));
        assertTrue(sent.getSubject().contains("Neue Buchungsanfrage"));
        assertTrue(sent.getHtml().contains("Halle A"));
    }

    @Test
    void notifyAdminsAboutNewBookingSeriesRequest_skips_when_no_admin_exists() {
        when(userRepository.findAllActive()).thenReturn(List.of(user(Role.CLUB_REPRESENTATIVE, "rep@example.com")));

        adapter.notifyAdminsAboutNewBookingSeriesRequest(seriesRequest(user(Role.CLUB_REPRESENTATIVE, "rep@example.com"), hall("Halle A")));

        verify(mailer, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void notifyRequester_paths_include_reason_conditionally() {
        User requester = user(Role.CLUB_REPRESENTATIVE, "rep@example.com");
        Hall hall = hall("Halle A");

        adapter.notifyRequesterAboutBookingRequestRejected(bookingRequest(requester, hall), null);
        adapter.notifyRequesterAboutBookingRequestRejected(bookingRequest(requester, hall), "Konflikt");

        ArgumentCaptor<Mail[]> captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer, org.mockito.Mockito.times(2)).send(captor.capture());

        Mail withoutReason = captor.getAllValues().get(0)[0];
        Mail withReason = captor.getAllValues().get(1)[0];

        assertFalse(withoutReason.getHtml().contains("Begründung:"));
        assertTrue(withReason.getHtml().contains("Begründung: Konflikt"));
    }

    @Test
    void notifyRequester_approved_and_update_and_cancelled_use_requester_email() {
        User requester = user(Role.CLUB_REPRESENTATIVE, "rep@example.com");
        Hall hall = hall("Halle A");

        adapter.notifyRequesterAboutBookingRequestApproved(bookingRequest(requester, hall), booking(requester, hall));
        adapter.notifyRequesterAboutBookingUpdated(booking(requester, hall));
        adapter.notifyRequesterAboutBookingSeriesCancelledByAdmin(series(requester, hall), "Grund");

        ArgumentCaptor<Mail[]> captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer, org.mockito.Mockito.times(3)).send(captor.capture());

        for (Mail[] sentArray : captor.getAllValues()) {
            assertEquals("rep@example.com", sentArray[0].getTo().get(0));
        }
    }
}

