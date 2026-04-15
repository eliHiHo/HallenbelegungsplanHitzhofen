import { api } from "./client";

export const bookingSeriesApi = {
  // reason is optional; passed as query parameter (not body)
  cancelSeries: (seriesId: string, reason?: string) => {
    const qs = reason ? `?reason=${encodeURIComponent(reason)}` : "";
    return api.delete<void>(`/booking-series/${seriesId}${qs}`);
  },

  // Cancel a single occurrence of a series.
  // Requires both the seriesId and the bookingId (the individual booking's ID).
  cancelOccurrence: (seriesId: string, bookingId: string, reason?: string) => {
    const qs = reason ? `?reason=${encodeURIComponent(reason)}` : "";
    return api.delete<void>(`/booking-series/${seriesId}/occurrences/${bookingId}${qs}`);
  },
};
