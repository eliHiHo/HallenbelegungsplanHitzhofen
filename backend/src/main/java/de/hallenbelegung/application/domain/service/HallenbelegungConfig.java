package de.hallenbelegung.application.domain.service;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalTime;

@ApplicationScoped
public class HallenbelegungConfig {

    @ConfigProperty(name = "hallen.opening.start")
    String openingStartRaw;

    @ConfigProperty(name = "hallen.opening.end")
    String openingEndRaw;

    @ConfigProperty(name = "hallen.booking.interval-minutes")
    int bookingIntervalMinutes;

    public int bookingIntervalMinutes() {
        return bookingIntervalMinutes;
    }

    public LocalTime openingStart() {
        return LocalTime.parse(openingStartRaw);
    }

    public LocalTime openingEnd() {
        return LocalTime.parse(openingEndRaw);
    }

}

