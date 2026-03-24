package de.hallenbelegung.application.domain.port.out;

import java.time.LocalTime;

public interface HallConfigPort {
    int bookingIntervalMinutes();
    LocalTime openingStart();
    LocalTime openingEnd();
}