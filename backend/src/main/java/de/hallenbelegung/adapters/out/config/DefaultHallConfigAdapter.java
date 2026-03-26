package de.hallenbelegung.adapters.out.config;

import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalTime;

@ApplicationScoped
public class DefaultHallConfigAdapter implements HallConfigPort {
    @Override public int bookingIntervalMinutes() { return 15; }
    @Override public LocalTime openingStart() { return LocalTime.of(8, 0); }
    @Override public LocalTime openingEnd() { return LocalTime.of(22, 0); }
}
