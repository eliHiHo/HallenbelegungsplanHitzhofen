package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;
import de.hallenbelegung.application.domain.model.*;
import de.hallenbelegung.application.domain.port.out.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookingSeriesRequestServiceTest {

    @Test
    public void approveReturnsCreatedAndSkipped() {
        BookingSeriesRequestRepositoryPort seriesRepo = mock(BookingSeriesRequestRepositoryPort.class);
        BookingSeriesRepositoryPort seriesSavedRepo = mock(BookingSeriesRepositoryPort.class);
        BookingRepositoryPort bookingRepo = mock(BookingRepositoryPort.class);
        BlockedTimeRepositoryPort blockedRepo = mock(BlockedTimeRepositoryPort.class);
        UserRepositoryPort userRepo = mock(UserRepositoryPort.class);
        HallRepositoryPort hallRepo = mock(HallRepositoryPort.class);
        HallConfigPort config = mock(HallConfigPort.class);
        Clock clock = Clock.systemDefaultZone();
        NotificationPort notification = mock(NotificationPort.class);

        // --- FIX: save darf nicht null zurückgeben ---
        when(bookingRepo.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // --- Testdaten ---
        User admin = User.createNew("A", "Admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepo.findById(any())).thenReturn(Optional.of(admin));

        Hall hall = Hall.createNew("Halle", "desc", HallType.FULL);
        when(hallRepo.findById(any())).thenReturn(Optional.of(hall));

        BookingSeriesRequest request = BookingSeriesRequest.createNew(
                "T", "D",
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                hall,
                admin
        );

        when(seriesRepo.findById(any())).thenReturn(Optional.of(request));

        BookingSeriesRequestService svc = new BookingSeriesRequestService(
                seriesRepo,
                seriesSavedRepo,
                bookingRepo,
                blockedRepo,
                userRepo,
                hallRepo,
                config,
                clock,
                notification
        );

        // --- Execute ---
        BookingSeriesApproveResult res = svc.approve(admin.getId(), request.getId());

        // --- Assertions ---
        assertNotNull(res);
        assertNotNull(res.createdBookingIds);
        assertNotNull(res.skippedOccurrences);
    }
}