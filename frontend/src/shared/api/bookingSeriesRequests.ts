import { api } from "./client";
import type {
  BookingSeriesRequest,
  BookingSeriesRequestCreate,
  BookingSeriesApproveResult,
} from "../types/api";

export const bookingSeriesRequestsApi = {
  list: () =>
    api.get<BookingSeriesRequest[]>("/booking-series-requests"),

  listOpen: () =>
    api.get<BookingSeriesRequest[]>("/booking-series-requests?open=true"),

  get: (id: string) =>
    api.get<BookingSeriesRequest>(`/booking-series-requests/${id}`),

  create: (data: BookingSeriesRequestCreate) =>
    api.post<void>("/booking-series-requests", {
      title: data.title,
      description: data.description,
      weekday: data.weekday,
      startTime: data.startTime,
      endTime: data.endTime,
      startDate: data.startDate,
      endDate: data.endDate,
      hallId: data.hallId,
    }),

  approve: (id: string) =>
    api.post<BookingSeriesApproveResult>(`/booking-series-requests/${id}/approve`),

  reject: (id: string, reason: string) =>
    api.post<void>(`/booking-series-requests/${id}/reject`, { reason }),
};
