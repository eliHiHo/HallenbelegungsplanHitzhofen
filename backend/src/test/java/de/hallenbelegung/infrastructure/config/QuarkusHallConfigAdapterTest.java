package de.hallenbelegung.infrastructure.config;

import de.hallenbelegung.adapters.out.config.DefaultHallConfigAdapter;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarkusHallConfigAdapterTest {

    @Test
    void bookingIntervalMinutes_openingStart_openingEnd_return_default_values() {
        HallConfigPort adapter = new DefaultHallConfigAdapter();

        assertEquals(15, adapter.bookingIntervalMinutes());
        assertEquals(LocalTime.of(8, 0), adapter.openingStart());
        assertEquals(LocalTime.of(22, 0), adapter.openingEnd());
    }

    @Test
    void default_values_are_applied_when_no_override_is_present() {
        HallConfigPort adapter = new DefaultHallConfigAdapter();

        assertEquals(15, adapter.bookingIntervalMinutes());
        assertEquals(LocalTime.of(8, 0), adapter.openingStart());
        assertEquals(LocalTime.of(22, 0), adapter.openingEnd());
    }
}

