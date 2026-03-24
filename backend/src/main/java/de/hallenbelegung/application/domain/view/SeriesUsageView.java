package de.hallenbelegung.application.domain.view;

import java.util.UUID;

public class SeriesUsageView {

    private final UUID bookingSeriesId;
    private final String title;
    private final long bookingCount;

    public SeriesUsageView(UUID bookingSeriesId, String title, long bookingCount) {
        this.bookingSeriesId = bookingSeriesId;
        this.title = title;
        this.bookingCount = bookingCount;
    }

    public UUID getBookingSeriesId() {
        return bookingSeriesId;
    }

    public String getTitle() {
        return title;
    }

    public long getBookingCount() {
        return bookingCount;
    }
}