import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingsApi, type BookingFeedbackPayload } from "../../shared/api/bookings";

export function useFeedbackBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: BookingFeedbackPayload }) =>
      bookingsApi.submitFeedback(id, payload),
    onSuccess: (_data, { id }) => {
      // Invalidate the cached booking detail and the calendar view
      void queryClient.invalidateQueries({ queryKey: ["bookings", id] });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}
