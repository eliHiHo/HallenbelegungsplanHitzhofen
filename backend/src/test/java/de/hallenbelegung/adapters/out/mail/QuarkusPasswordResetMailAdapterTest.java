package de.hallenbelegung.adapters.out.mail;

import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QuarkusPasswordResetMailAdapterTest {

    @Test
    void sendPasswordResetMail_sends_expected_html_mail() {
        Mailer mailer = mock(Mailer.class);
        QuarkusPasswordResetMailAdapter adapter = new QuarkusPasswordResetMailAdapter(mailer, "https://example.org");

        User user = new User(
                UUID.randomUUID(),
                "Max",
                "Mustermann",
                "max@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.now(),
                Instant.now()
        );

        adapter.sendPasswordResetMail(user, "token-123");

        ArgumentCaptor<Mail[]> captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer).send(captor.capture());

        Mail sent = captor.getValue()[0];
        assertEquals("max@example.com", sent.getTo().get(0));
        assertEquals("Passwort zurücksetzen – Hallenbelegungsplan Hitzhofen", sent.getSubject());
        assertTrue(sent.getHtml().contains("Hallo Max"));
        assertTrue(sent.getHtml().contains("https://example.org/reset-password?token=token-123"));
        assertTrue(sent.getHtml().contains("2 Stunden"));
    }
}

