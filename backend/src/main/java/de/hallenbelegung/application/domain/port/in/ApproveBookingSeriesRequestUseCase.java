package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;

import java.util.UUID;

public interface ApproveBookingSeriesRequestUseCase {
    BookingSeriesApproveResult approve(UUID adminUserId, UUID bookingSeriesRequestId);
}