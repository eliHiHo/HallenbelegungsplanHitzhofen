package de.hallenbelegung.adapters.in.api.rest;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
@Path("/test/mail")
@Produces(MediaType.APPLICATION_JSON)
public class TestMailResource {

    private static final Logger LOG = Logger.getLogger(TestMailResource.class);
    private static final String TEST_RECIPIENT = "hallenbelegungsplaner@gmail.com";

    private final Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.host", defaultValue = "<nicht gesetzt>")
    String mailerHost;

    @ConfigProperty(name = "quarkus.mailer.port", defaultValue = "0")
    int mailerPort;

    @ConfigProperty(name = "quarkus.mailer.username")
    Optional<String> mailerUsername;

    @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "<nicht gesetzt>")
    String mailerFrom;

    @ConfigProperty(name = "quarkus.mailer.start-tls", defaultValue = "<nicht gesetzt>")
    String mailerStartTls;

    @ConfigProperty(name = "quarkus.mailer.mock", defaultValue = "false")
    boolean mailerMock;

    public TestMailResource(Mailer mailer) {
        this.mailer = mailer;
    }

    @POST
    public Response sendTestMail() {
        String usernameDisplay = mailerUsername
                .map(u -> u.isEmpty() ? "<leer>" : u)
                .orElse("<nicht gesetzt>");

        LOG.infof("=== Mailer-Konfiguration zur Laufzeit ===");
        LOG.infof("  host      : %s", mailerHost);
        LOG.infof("  port      : %d", mailerPort);
        LOG.infof("  username  : %s", usernameDisplay);
        LOG.infof("  from      : %s", mailerFrom);
        LOG.infof("  start-tls : %s", mailerStartTls);
        LOG.infof("  mock      : %b", mailerMock);
        LOG.infof("=========================================");
        LOG.infof("Test-Mailversand gestartet an %s", TEST_RECIPIENT);

        try {
            mailer.send(Mail.withText(
                    TEST_RECIPIENT,
                    "Test-Mail Hallenbelegungsplaner",
                    "Dies ist eine Test-Mail. Wenn du diese erhältst, funktioniert der SMTP-Versand korrekt.\n\nTimestamp: " + Instant.now()
            ));
            LOG.infof("Test-Mailversand erfolgreich an %s", TEST_RECIPIENT);
            return Response.ok("{\"message\":\"Test-Mail erfolgreich gesendet\"}").build();
        } catch (Exception e) {
            LOG.errorf(e, "Test-Mailversand fehlgeschlagen: %s", e.getMessage());
            return Response.serverError()
                    .entity("{\"error\":\"Mailversand fehlgeschlagen: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
