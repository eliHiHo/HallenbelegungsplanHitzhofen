package de.hallenbelegung.adapters.out.mail;

import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.PasswordResetMailPort;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class QuarkusPasswordResetMailAdapter implements PasswordResetMailPort {

    private final Mailer mailer;
    private final String baseUrl;

    @ConfigProperty(name = "app.mail.enabled", defaultValue = "false")
    boolean mailEnabled;

    public QuarkusPasswordResetMailAdapter(
            Mailer mailer,
            @ConfigProperty(name = "app.base-url") String baseUrl
    ) {
        this.mailer = mailer;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendPasswordResetMail(User user, String resetToken) {
        if (!mailEnabled) return;

        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        mailer.send(Mail.withHtml(
                user.getEmail(),
                "Passwort zurücksetzen – Hallenbelegungsplan Hitzhofen",
                "<p>Hallo " + esc(user.getFirstName()) + ",</p>" +
                        "<p>du hast eine Anfrage zum Zurücksetzen deines Passworts gestellt.</p>" +
                        "<p><a href=\"" + resetLink + "\">Hier klicken um Passwort zurückzusetzen</a></p>" +
                        "<p>Dieser Link ist 2 Stunden gültig.</p>" +
                        "<p>Falls du diese Anfrage nicht gestellt hast, kannst du diese E-Mail ignorieren.</p>"
        ));
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
