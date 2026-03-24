package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeries;
import java.util.List;

public interface GetUserBookingSeriesUseCase {
    List<BookingSeries> getSeriesByUser(Long userId);
}