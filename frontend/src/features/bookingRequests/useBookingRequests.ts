import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingRequestsApi } from "../../shared/api/bookingRequests";
import type { BookingRequestCreate } from "../../shared/types/api";

// ["booking-requests"] is a shared prefix: RQ v5 invalidateQueries uses prefix
// matching by default, so invalidating this key also covers OPEN_BOOKING_REQUESTS_KEY.
// We keep OPEN_BOOKING_REQUESTS_KEY explicit in mutations that directly affect the
// admin view so the intent is clear at the call site.
export const BOOKING_REQUESTS_KEY = ["booking-requests"];
export const OPEN_BOOKING_REQUESTS_KEY = ["booking-requests", "open"];

export function useBookingRequests() {
  return useQuery({
    queryKey: BOOKING_REQUESTS_KEY,
    queryFn: bookingRequestsApi.list,
  });
}

export function useOpenBookingRequests() {
  return useQuery({
    queryKey: OPEN_BOOKING_REQUESTS_KEY,
    queryFn: bookingRequestsApi.listOpen,
  });
}

export function useCreateBookingRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BookingRequestCreate) => bookingRequestsApi.create(data),
    onSuccess: () => {
      // Invalidates club-rep list and, via prefix, the admin open-requests list
      void queryClient.invalidateQueries({ queryKey: BOOKING_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

export function useApproveBookingRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => bookingRequestsApi.approve(id),
    onSuccess: () => {
      // Explicit: both the admin open-requests list and the club-rep full list
      void queryClient.invalidateQueries({ queryKey: OPEN_BOOKING_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: BOOKING_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

export function useRejectBookingRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      bookingRequestsApi.reject(id, reason),
    onSuccess: () => {
      // Explicit: both the admin open-requests list and the club-rep full list
      void queryClient.invalidateQueries({ queryKey: OPEN_BOOKING_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: BOOKING_REQUESTS_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}
