package de.hallenbelegung.adapters.out.clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

@ApplicationScoped
public class SystemClockProducer {
    @Produces
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
