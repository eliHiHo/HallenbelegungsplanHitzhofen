package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.DeleteBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.GetBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
@Transactional
public class BlockedTimeService implements
        CreateBlockedTimeUseCase,
        GetBlockedTimeUseCase,
        DeleteBlockedTimeUseCase {

    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final BookingRepositoryPort bookingRepository;
    private final UserRepositoryPort userRepository;
    private final HallRepositoryPort hallRepository;
    private final HallenbelegungConfig config;
    private final Clock clock;

    public BlockedTimeService(
            BlockedTimeRepositoryPort blockedTimeRepository,
            BookingRepositoryPort bookingRepository,
            UserRepositoryPort userRepository,
            HallRepositoryPort hallRepository,
            HallenbelegungConfig config,
            Clock clock
    ) {
        this.blockedTimeRepository = blockedTimeRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.hallRepository = hallRepository;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public void create(Long hallId,
                       String reason,
                       LocalDateTime startTime,
                       LocalDateTime endTime,
                       Long adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to create blocked times");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        validateCreateInput(reason, startTime, endTime);
        validateTimeGrid(startTime, endTime);
        validateOpeningHours(startTime.toLocalTime(), endTime.toLocalTime());

        checkForBookingConflicts(hall, startTime, endTime);
        checkForBlockedTimeConflicts(hall, startTime, endTime);

        BlockedTime blockedTime = BlockedTime.createNew(
                reason,
                startTime,
                endTime,
                hall
        );

        blockedTimeRepository.save(blockedTime);
    }

    private void validateCreateInput(String reason,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required");
        }

        if (startTime == null || endTime == null) {
            throw new ValidationException("Start time and end time are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Start time must be before end time");
        }

        if (startTime.isBefore(LocalDateTime.now(clock))) {
            throw new ValidationException("Blocked time cannot start in the past");
        }
    }

    private void validateTimeGrid(LocalDateTime startTime, LocalDateTime endTime) {
        int interval = config.bookingIntervalMinutes();

        if (startTime.getMinute() % interval != 0 || endTime.getMinute() % interval != 0) {
            throw new ValidationException("Times must match the allowed booking interval");
        }

        if (startTime.getSecond() != 0 || endTime.getSecond() != 0
                || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new ValidationException("Seconds and nanoseconds are not allowed");
        }
    }

    private void validateOpeningHours(LocalTime start, LocalTime end) {
        if (start.isBefore(config.openingStart()) || end.isAfter(config.openingEnd())) {
            throw new ValidationException("Blocked time is outside opening hours");
        }
    }

    private void checkForBookingConflicts(Hall requestedHall,
                                          LocalDateTime start,
                                          LocalDateTime end) {

        if (requestedHall.isFullHall()) {
            boolean bookingConflict = !bookingRepository.findByTimeRange(start, end).isEmpty();
            if (bookingConflict) {
                throw new BookingConflictException("Conflict with existing booking");
            }
            return;
        }

        boolean sameHallConflict = !bookingRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallConflict) {
            throw new BookingConflictException("Conflict with existing booking");
        }

        boolean fullHallConflict = bookingRepository
                .findByTimeRange(start, end)
                .stream()
                .anyMatch(booking -> booking.getHall().isFullHall());

        if (fullHallConflict) {
            throw new BookingConflictException("Conflict with full hall booking");
        }
    }

    private void checkForBlockedTimeConflicts(Hall requestedHall,
                                              LocalDateTime start,
                                              LocalDateTime end) {

        if (requestedHall.isFullHall()) {
            boolean blockedConflict = !blockedTimeRepository.findAllByTimeRange(start, end).isEmpty();
            if (blockedConflict) {
                throw new BookingConflictException("Conflict with existing blocked time");
            }
            return;
        }

        boolean sameHallConflict = !blockedTimeRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallConflict) {
            throw new BookingConflictException("Conflict with existing blocked time");
        }

        boolean fullHallConflict = blockedTimeRepository
                .findAllByTimeRange(start, end)
                .stream()
                .anyMatch(blockedTime -> blockedTime.getHall().isFullHall());

        if (fullHallConflict) {
            throw new BookingConflictException("Conflict with full hall blocked time");
        }
    }

    public List<BlockedTime> getAll(Long adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view blocked times");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        return blockedTimeRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getStartDateTime().compareTo(b.getStartDateTime()))
                .toList();
    }

    public void delete(Long blockedTimeId, Long adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to delete blocked times");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        blockedTimeRepository.findById(blockedTimeId)
                .orElseThrow(() -> new NotFoundException("Blocked time not found"));

        blockedTimeRepository.deleteById(blockedTimeId);
    }

    public BlockedTime getById(Long adminUserId, Long blockedTimeId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view blocked times");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        return blockedTimeRepository.findById(blockedTimeId)
                .orElseThrow(() -> new NotFoundException("Blocked time not found"));
    }
}