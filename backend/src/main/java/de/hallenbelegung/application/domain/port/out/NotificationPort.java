package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

public interface NotificationPort {

    void notifyAdminsAboutNewBookingRequest(BookingRequest request);

    void notifyAdminsAboutNewBookingSeriesRequest(BookingSeriesRequest request);

    void notifyRequesterAboutBookingRequestApproved(BookingRequest request, Booking booking);

    void notifyRequesterAboutBookingSeriesRequestApproved(BookingSeriesRequest request, BookingSeries bookingSeries);

    void notifyRequesterAboutBookingRequestRejected(BookingRequest request, String rejectionReason);

    void notifyRequesterAboutBookingSeriesRequestRejected(BookingSeriesRequest request, String rejectionReason);

    void notifyRequesterAboutBookingCancelledByAdmin(Booking booking, String cancellationReason);

    void notifyRequesterAboutBookingSeriesCancelledByAdmin(BookingSeries bookingSeries, String cancellationReason);
}