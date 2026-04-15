import { api } from "./client";
import type { Booking } from "../types/api";

export interface BookingFeedbackPayload {
  participantCount?: number | null;
  comment?: string | null;
}

export const bookingsApi = {
  get: (id: string) =>
    api.get<Booking>(`/bookings/${id}`),

  // reason is optional; passed as query parameter (not body)
  cancel: (id: string, reason?: string) => {
    const qs = reason ? `?reason=${encodeURIComponent(reason)}` : "";
    return api.delete<void>(`/bookings/${id}${qs}`);
  },

  submitFeedback: (id: string, payload: BookingFeedbackPayload) =>
    api.put<void>(`/bookings/${id}/feedback`, {
      participantCount: payload.participantCount ?? null,
      comment: payload.comment ?? null,
    }),
};
