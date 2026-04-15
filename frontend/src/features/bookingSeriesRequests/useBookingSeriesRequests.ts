import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingSeriesRequestsApi } from "../../shared/api/bookingSeriesRequests";
import type { BookingSeriesRequestCreate } from "../../shared/types/api";

export const SERIES_REQUESTS_KEY = ["booking-series-requests"];
export const OPEN_SERIES_REQUESTS_KEY = ["booking-series-requests", "open"];

export function useBookingSeriesRequests() {
  return useQuery({
    queryKey: SERIES_REQUESTS_KEY,
    queryFn: bookingSeriesRequestsApi.list,
  });
}

export function useOpenBookingSeriesRequests() {
  return useQuery({
    queryKey: OPEN_SERIES_REQUESTS_KEY,
    queryFn: bookingSeriesRequestsApi.listOpen,
  });
}

export function useCreateBookingSeriesRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BookingSeriesRequestCreate) =>
      bookingSeriesRequestsApi.create(data),
    onSuccess: () => {
      // SERIES_REQUESTS_KEY prefix also covers OPEN_SERIES_REQUESTS_KEY
      void queryClient.invalidateQueries({ queryKey: SERIES_REQUESTS_KEY });
    },
  });
}

export function useApproveBookingSeriesRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => bookingSeriesRequestsApi.approve(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: OPEN_SERIES_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: SERIES_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

export function useRejectBookingSeriesRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      bookingSeriesRequestsApi.reject(id, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: OPEN_SERIES_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: SERIES_REQUESTS_KEY });
    },
  });
}
