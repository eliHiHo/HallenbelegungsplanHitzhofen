import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";

export function useCancelBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      bookingsApi.cancel(id, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}
