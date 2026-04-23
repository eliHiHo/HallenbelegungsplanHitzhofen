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

    @ConfigProperty(name = "app.mail.enabled", defaultValue = "false")
    boolean mailEnabled;

    public QuarkusNotificationAdapter(Mailer mailer, UserRepositoryPort userRepository) {
        this.mailer = mailer;
        this.userRepository = userRepository;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Override
    public void notifyAdminsAboutNewBookingRequest(BookingRequest request) {
        if (!mailEnabled) return;

        List<String> adminEmails = userRepository.findAllActive().stream()
                .filter(User::isAdmin)
                .map(User::getEmail)
                .toList();

        if (adminEmails.isEmpty()) return;

        String subject = "Neue Buchungsanfrage: " + esc(request.getTitle());
        String body = "<p>Eine neue Buchungsanfrage wurde gestellt:</p>"
                + "<ul>"
                + "<li>Titel: " + esc(request.getTitle()) + "</li>"
                + "<li>Von: " + esc(request.getRequestedBy().getFullName()) + "</li>"
                + "<li>Start: " + request.getStartAt() + "</li>"
                + "<li>Ende: " + request.getEndAt() + "</li>"
                + "<li>Halle: " + esc(request.getHall().getName()) + "</li>"
                + "</ul>";

        for (String email : adminEmails) {
            mailer.send(Mail.withHtml(email, subject, body));
        }
    }

    @Override
    public void notifyAdminsAboutNewBookingSeriesRequest(BookingSeriesRequest request) {
        if (!mailEnabled) return;

        List<String> adminEmails = userRepository.findAllActive().stream()
                .filter(User::isAdmin)
                .map(User::getEmail)
                .toList();

        if (adminEmails.isEmpty()) return;

        String subject = "Neue Serienanfrage: " + esc(request.getTitle());
        String body = "<p>Eine neue Serienanfrage wurde gestellt von "
                + esc(request.getRequestedBy().getFullName()) + ".</p>"
                + "<p>Halle: " + esc(request.getHall().getName()) + ", "
                + "Wochentag: " + request.getWeekday() + ", "
                + "von " + request.getStartDate() + " bis " + request.getEndDate() + "</p>";

        for (String email : adminEmails) {
            mailer.send(Mail.withHtml(email, subject, body));
        }
    }

    @Override
    public void notifyRequesterAboutBookingRequestApproved(BookingRequest request, Booking booking) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Buchungsanfrage genehmigt: " + esc(request.getTitle()),
                "<p>Deine Buchungsanfrage wurde genehmigt.</p>"
                        + "<p>Termin: " + booking.getStartAt() + " – " + booking.getEndAt() + "</p>"
                        + "<p>Halle: " + esc(booking.getHall().getName()) + "</p>"
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesRequestApproved(BookingSeriesRequest request, BookingSeries series) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Serienanfrage genehmigt: " + esc(request.getTitle()),
                "<p>Deine Serienanfrage wurde genehmigt.</p>"
                        + "<p>Halle: " + esc(series.getHall().getName()) + ", "
                        + "jeden " + series.getWeekday()
                        + " von " + series.getStartDate() + " bis " + series.getEndDate() + ".</p>"
        ));
    }

    @Override
    public void notifyRequesterAboutBookingRequestRejected(BookingRequest request, String reason) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Buchungsanfrage abgelehnt: " + esc(request.getTitle()),
                "<p>Deine Buchungsanfrage wurde leider abgelehnt.</p>"
                        + (reason != null ? "<p>Begründung: " + esc(reason) + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesRequestRejected(BookingSeriesRequest request, String reason) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                request.getRequestedBy().getEmail(),
                "Serienanfrage abgelehnt: " + esc(request.getTitle()),
                "<p>Deine Serienanfrage wurde leider abgelehnt.</p>"
                        + (reason != null ? "<p>Begründung: " + esc(reason) + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingCancelledByAdmin(Booking booking, String reason) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                booking.getResponsibleUser().getEmail(),
                "Buchung storniert: " + esc(booking.getTitle()),
                "<p>Deine Buchung am " + booking.getStartAt().toLocalDate()
                        + " wurde von der Verwaltung storniert.</p>"
                        + (reason != null ? "<p>Begründung: " + esc(reason) + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingSeriesCancelledByAdmin(BookingSeries series, String reason) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                series.getResponsibleUser().getEmail(),
                "Terminserie storniert: " + esc(series.getTitle()),
                "<p>Deine Terminserie wurde von der Verwaltung storniert.</p>"
                        + (reason != null ? "<p>Begründung: " + esc(reason) + "</p>" : "")
        ));
    }

    @Override
    public void notifyRequesterAboutBookingUpdated(Booking booking) {
        if (!mailEnabled) return;

        mailer.send(Mail.withHtml(
                booking.getResponsibleUser().getEmail(),
                "Buchung geändert: " + esc(booking.getTitle()),
                "<p>Deine Buchung wurde von der Verwaltung angepasst.</p>"
                        + "<p>Neuer Termin: " + booking.getStartAt() + " – " + booking.getEndAt() + "</p>"
        ));
    }
}
