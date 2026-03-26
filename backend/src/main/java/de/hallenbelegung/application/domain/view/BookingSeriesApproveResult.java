package de.hallenbelegung.application.domain.view;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BookingSeriesApproveResult {
    public final List<UUID> createdBookingIds;
    public final List<LocalDate> skippedOccurrences;

    public BookingSeriesApproveResult(List<UUID> createdBookingIds, List<LocalDate> skippedOccurrences) {
        this.createdBookingIds = createdBookingIds;
        this.skippedOccurrences = skippedOccurrences;
    }
}
