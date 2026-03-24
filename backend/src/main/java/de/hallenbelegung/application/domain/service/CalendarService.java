package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCalendarDayUseCase;
import de.hallenbelegung.application.domain.port.in.GetCalendarWeekUseCase;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.CalendarDayView;
import de.hallenbelegung.application.domain.view.CalendarEntryType;
import de.hallenbelegung.application.domain.view.CalendarEntryView;
import de.hallenbelegung.application.domain.view.CalendarWeekView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
@Transactional
public class CalendarService implements GetCalendarWeekUseCase, GetCalendarDayUseCase {

    private final BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository;
    private final BookingRepositoryPort bookingRepository;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final BookingRequestRepositoryPort bookingRequestRepository;
    private final UserRepositoryPort userRepository;

    public CalendarService(
            BookingRepositoryPort bookingRepository,
            BlockedTimeRepositoryPort blockedTimeRepository,
            BookingRequestRepositoryPort bookingRequestRepository,
            UserRepositoryPort userRepository,
            BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.userRepository = userRepository;
        this.bookingSeriesRequestRepository = bookingSeriesRequestRepository;
    }

    @Override
    public CalendarWeekView getWeek(LocalDate weekStart, Long userId) {
        User currentUser = resolveCurrentUser(userId);

        LocalDate normalizedWeekStart = weekStart;
        LocalDateTime start = normalizedWeekStart.atStartOfDay();
        LocalDateTime end = normalizedWeekStart.plusDays(7).atStartOfDay();

        List<CalendarEntryView> entries = loadEntriesForRange(start, end, currentUser);

        return new CalendarWeekView(
                normalizedWeekStart,
                normalizedWeekStart.plusDays(6),
                entries
        );
    }

    @Override
    public CalendarDayView getDay(LocalDate day, Long userId) {
        User currentUser = resolveCurrentUser(userId);

        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        List<CalendarEntryView> entries = loadEntriesForRange(start, end, currentUser);

        return new CalendarDayView(day, entries);
    }

    private User resolveCurrentUser(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private List<CalendarEntryView> loadEntriesForRange(
            LocalDateTime start,
            LocalDateTime end,
            User currentUser
    ) {
        List<CalendarEntryView> entries = new ArrayList<>();

        List<Booking> bookings = bookingRepository.findByTimeRange(start, end);
        for (Booking booking : bookings) {
            if (isInRange(booking.getStartDateTime(), booking.getEndDateTime(), start, end)) {
                entries.add(mapBookingToEntry(booking, currentUser));
            }
        }

        List<BlockedTime> blockedTimes = blockedTimeRepository.findAllByTimeRange(start, end);
        for (BlockedTime blockedTime : blockedTimes) {
            if (isInRange(blockedTime.getStartDateTime(), blockedTime.getEndDateTime(), start, end)) {
                entries.add(mapBlockedTimeToEntry(blockedTime));
            }
        }

        if (currentUser != null) {
            List<BookingRequest> openRequests = bookingRequestRepository.findByStatus(BookingRequestStatus.OPEN);

            for (BookingRequest request : openRequests) {
                if (canSeeBookingRequest(request, currentUser)
                        && isInRange(request.getStartDateTime(), request.getEndDateTime(), start, end)) {
                    entries.add(mapBookingRequestToEntry(request, currentUser));
                }
            }

            List<BookingSeriesRequest> openSeriesRequests =
                    bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.OPEN);

            for (BookingSeriesRequest request : openSeriesRequests) {
                if (canSeeBookingSeriesRequest(request, currentUser)
                        && isSeriesRequestInRange(request, start, end)) {
                    entries.add(mapBookingSeriesRequestToEntry(request, currentUser, start, end));
                }
            }
        }

        return entries.stream()
                .sorted(Comparator.comparing(CalendarEntryView::startDateTime))
                .toList();
    }

    private boolean canSeeBookingRequest(BookingRequest request, User currentUser) {
        if (currentUser.isAdmin()) {
            return true;
        }

        return request.getRequestingUser().getId().equals(currentUser.getId());
    }

    private boolean canSeeBookingSeriesRequest(BookingSeriesRequest request, User currentUser) {
        if (currentUser.isAdmin()) {
            return true;
        }

        return request.getRequestingUser().getId().equals(currentUser.getId());
    }

    private CalendarEntryView mapBookingToEntry(Booking booking, User currentUser) {
        boolean ownEntry = currentUser != null
                && booking.getResponsibleUser().getId().equals(currentUser.getId());

        return new CalendarEntryView(
                booking.getId(),
                CalendarEntryType.BOOKING,
                booking.getTitle(),
                booking.getDescription(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                booking.getHall().getId(),
                booking.getHall().getName(),
                booking.getStatus().name(),
                ownEntry
        );
    }

    private CalendarEntryView mapBlockedTimeToEntry(BlockedTime blockedTime) {
        return new CalendarEntryView(
                blockedTime.getId(),
                CalendarEntryType.BLOCKED_TIME,
                blockedTime.getReason(),
                null,
                blockedTime.getStartDateTime(),
                blockedTime.getEndDateTime(),
                blockedTime.getHall().getId(),
                blockedTime.getHall().getName(),
                "BLOCKED",
                false
        );
    }

    private CalendarEntryView mapBookingRequestToEntry(BookingRequest request, User currentUser) {
        boolean ownEntry = currentUser != null
                && request.getRequestingUser().getId().equals(currentUser.getId());

        return new CalendarEntryView(
                request.getId(),
                CalendarEntryType.BOOKING_REQUEST,
                request.getTitle(),
                request.getDescription(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getHall().getId(),
                request.getHall().getName(),
                request.getStatus().name(),
                ownEntry
        );
    }

    private CalendarEntryView mapBookingSeriesRequestToEntry(
            BookingSeriesRequest request,
            User currentUser,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        boolean ownEntry = currentUser != null
                && request.getRequestingUser().getId().equals(currentUser.getId());

        LocalDate firstOccurrence = findFirstOccurrenceInRange(
                request,
                rangeStart.toLocalDate(),
                rangeEnd.minusDays(1).toLocalDate()
        );

        if (firstOccurrence == null) {
            throw new IllegalStateException("Booking series request has no occurrence in requested calendar range");
        }

        return new CalendarEntryView(
                request.getId(),
                CalendarEntryType.BOOKING_SERIES_REQUEST,
                request.getTitle(),
                request.getDescription(),
                firstOccurrence.atTime(request.getStartTime()),
                firstOccurrence.atTime(request.getEndTime()),
                request.getHall().getId(),
                request.getHall().getName(),
                request.getStatus().name(),
                ownEntry
        );
    }

    private boolean isInRange(
            LocalDateTime entryStart,
            LocalDateTime entryEnd,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        return entryStart.isBefore(rangeEnd) && entryEnd.isAfter(rangeStart);
    }

    private boolean isSeriesRequestInRange(
            BookingSeriesRequest request,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        LocalDate weekStartDate = rangeStart.toLocalDate();
        LocalDate weekEndDate = rangeEnd.minusDays(1).toLocalDate();

        LocalDate firstPossibleDate = weekStartDate.isAfter(request.getStartDate())
                ? weekStartDate
                : request.getStartDate();

        LocalDate lastPossibleDate = weekEndDate.isBefore(request.getEndDate())
                ? weekEndDate
                : request.getEndDate();

        if (firstPossibleDate.isAfter(lastPossibleDate)) {
            return false;
        }

        for (LocalDate date = firstPossibleDate; !date.isAfter(lastPossibleDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek().equals(request.getWeekday())) {
                return true;
            }
        }

        return false;
    }

    private LocalDate findFirstOccurrenceInRange(
            BookingSeriesRequest request,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        LocalDate effectiveStart = rangeStart.isAfter(request.getStartDate())
                ? rangeStart
                : request.getStartDate();

        LocalDate effectiveEnd = rangeEnd.isBefore(request.getEndDate())
                ? rangeEnd
                : request.getEndDate();

        if (effectiveStart.isAfter(effectiveEnd)) {
            return null;
        }

        for (LocalDate date = effectiveStart; !date.isAfter(effectiveEnd); date = date.plusDays(1)) {
            if (date.getDayOfWeek().equals(request.getWeekday())) {
                return date;
            }
        }

        return null;
    }
}