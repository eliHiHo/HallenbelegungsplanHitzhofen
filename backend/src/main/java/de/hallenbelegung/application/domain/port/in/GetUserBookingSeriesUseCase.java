package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeries;
import java.util.List;
import java.util.UUID;

public interface GetUserBookingSeriesUseCase {
    List<BookingSeries> getSeriesByUser(UUID userId);
}