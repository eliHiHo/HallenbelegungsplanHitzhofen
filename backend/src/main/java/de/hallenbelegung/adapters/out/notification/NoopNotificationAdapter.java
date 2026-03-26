package de.hallenbelegung.adapters.out.notification;

import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoopNotificationAdapter implements NotificationPort {
    @Override public void notifyAdminsAboutNewBookingRequest(BookingRequest request) {}
    @Override public void notifyAdminsAboutNewBookingSeriesRequest(BookingSeriesRequest request) {}
    @Override public void notifyRequesterAboutBookingRequestApproved(BookingRequest request, Booking booking) {}
    @Override public void notifyRequesterAboutBookingSeriesRequestApproved(BookingSeriesRequest request, BookingSeries bookingSeries) {}
    @Override public void notifyRequesterAboutBookingRequestRejected(BookingRequest request, String rejectionReason) {}
    @Override public void notifyRequesterAboutBookingSeriesRequestRejected(BookingSeriesRequest request, String rejectionReason) {}
    @Override public void notifyRequesterAboutBookingCancelledByAdmin(Booking booking, String cancellationReason) {}
    @Override public void notifyRequesterAboutBookingSeriesCancelledByAdmin(BookingSeries bookingSeries, String cancellationReason) {}
    @Override public void notifyRequesterAboutBookingUpdated(Booking savedBooking) {}
}
