package de.hallenbelegung.adapters.out.notification;

import de.hallenbelegung.application.domain.model.*;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class QuarkusNotificationAdapter implements NotificationPort {

    private final Mailer mailer;
    private final UserRepositoryPort userRepository;

    @ConfigProperty(name = "quarkus.mailer.from")
    String senderAddress;

    public QuarkusNotificationAdapter(Mailer mailer, UserRepositoryPort userRepository) {
        this.mailer = mailer;
        this.userRepository = userRepository;
    }

    @Override
    public void notifyAdminsAboutNewBookingRequest(BookingRequest request) {
        List<String> adminEmails = userRepository.findAllActive().stream()
                .filter(User::isAdmin)
                .map(User::getEmail)
                .toList();

        if (adminEmails.isEmpty()) return;

        String subject = "Neue Buchungsanfrage: " + request.getTitle();
        String body = "<p>Eine neue Buchungsanfrage wurde gestellt:</p>"
                + "<ul>"
                + "<li>Titel: " + request.getTitle() + "</li>"
                + "<li>Von: " + request.getRequestedBy().getFullName() + "</li>"
                + "<li>Start: " + request.getStartAt() + "</li>"
                + "<li>Ende: " + request.getEndAt() + "</li>"
                + "<li>Halle: " + request.getHall().getName() + "</li>"
                + "</ul>";

        for (String email : adminEmails) {
            mailer.send(Mail.withHtml(email, subject, body));
        }
    }

    @Override
    public void notifyAdminsAboutNewBookingSeriesRequest(BookingSeriesRequest request) {
        List<String> adminEmails = userRepository.findAllActive().stream()
                .filter(User::isAdmin)
                .map(User::getEmail)
                .toList();

        if (adminEmails.isEmpty()) return;

        String subject = "Neue Serienanfrage: " + request.getTitle();
        String body = "<p>Eine neue Serienanfrage wurde gestellt von "
                + request.getRequestedBy().getFullName() + ".</p>"
                + "<p>Halle: " + request.getHall().getName() + ", "
                + "Wochentag: " + request.getWeekday() + ", "
                + "von " + request.getStartDate() + " bis " + request.getEndDate() + "</p>";

        for (String email : adminEmails) {
            mailer.send(Mail.withHtml(email, subject, body));
        }
    }

    @Override
    public void notifyRequesterAboutBookingRequestApproved(BookingRequest request, Booking booking) {
        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Buchungsanfrage genehmigt: " + request.getTitle(),
                "<p>Deine Buchungsanfrage wurde genehmigt.</p>"
                        + "<p>Termin: " + booking.getStartAt() + " – " + booking.getEndAt() + "</p>"
                        + "<p>Halle: " + booking.getHall().getName() + "</p>"
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesRequestApproved(BookingSeriesRequest request, BookingSeries series) {
        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Serienanfrage genehmigt: " + request.getTitle(),
                "<p>Deine Serienanfrage wurde genehmigt.</p>"
                        + "<p>Die Termine wurden in den Kalender eingetragen.</p>"
        ));
    }

    @Override
    public void notifyRequesterAboutBookingRequestRejected(BookingRequest request, String reason) {
        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Buchungsanfrage abgelehnt: " + request.getTitle(),
                "<p>Deine Buchungsanfrage wurde leider abgelehnt.</p>"
                        + (reason != null ? "<p>Begründung: " + reason + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesRequestRejected(BookingSeriesRequest request, String reason) {
        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Serienanfrage abgelehnt: " + request.getTitle(),
                "<p>Deine Serienanfrage wurde leider abgelehnt.</p>"
                        + (reason != null ? "<p>Begründung: " + reason + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingCancelledByAdmin(Booking booking, String reason) {
        mailer.send(Mail.withHtml(
                booking.getResponsibleUser().getEmail(),
                "Buchung storniert: " + booking.getTitle(),
                "<p>Deine Buchung am " + booking.getStartAt().toLocalDate()
                        + " wurde von der Verwaltung storniert.</p>"
                        + (reason != null ? "<p>Begründung: " + reason + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesCancelledByAdmin(BookingSeries series, String reason) {
        mailer.send(Mail.withHtml(
                series.getResponsibleUser().getEmail(),
                "Terminserie storniert: " + series.getTitle(),
                "<p>Deine Terminserie wurde von der Verwaltung storniert.</p>"
                        + (reason != null ? "<p>Begründung: " + reason + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingUpdated(Booking booking) {
        mailer.send(Mail.withHtml(
                booking.getResponsibleUser().getEmail(),
                "Buchung geändert: " + booking.getTitle(),
                "<p>Deine Buchung wurde von der Verwaltung angepasst.</p>"
                        + "<p>Neuer Termin: " + booking.getStartAt() + " – " + booking.getEndAt() + "</p>"
        ));
    }
}