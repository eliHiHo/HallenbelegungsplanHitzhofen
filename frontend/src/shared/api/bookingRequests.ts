import { api } from "./client";
import type { BookingRequest, BookingRequestCreate } from "../types/api";

export const bookingRequestsApi = {
  list: () =>
    api.get<BookingRequest[]>("/booking-requests"),

  listOpen: () =>
    api.get<BookingRequest[]>("/booking-requests?open=true"),

  get: (id: string) =>
    api.get<BookingRequest>(`/booking-requests/${id}`),

  create: (data: BookingRequestCreate) =>
    // Backend accepts the full BookingRequestDTO shape; only the listed fields are used.
    api.post<void>("/booking-requests", {
      title: data.title,
      description: data.description,
      startDateTime: data.startDateTime,
      endDateTime: data.endDateTime,
      hallId: data.hallId,
    }),

  approve: (id: string) =>
    api.post<void>(`/booking-requests/${id}/approve`),

  reject: (id: string, reason: string) =>
    api.post<void>(`/booking-requests/${id}/reject`, { reason }),
};
