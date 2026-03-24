package de.hallenbelegung.application.domain.view;

public class SeriesUsageView {

    private final Long bookingSeriesId;
    private final String title;
    private final long bookingCount;

    public SeriesUsageView(Long bookingSeriesId, String title, long bookingCount) {
        this.bookingSeriesId = bookingSeriesId;
        this.title = title;
        this.bookingCount = bookingCount;
    }

    public Long getBookingSeriesId() {
        return bookingSeriesId;
    }

    public String getTitle() {
        return title;
    }

    public long getBookingCount() {
        return bookingCount;
    }
}