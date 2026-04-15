import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";
import type { BookingUpdate } from "../../shared/types/api";

export function useUpdateBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: BookingUpdate }) =>
      bookingsApi.update(id, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
      void queryClient.invalidateQueries({ queryKey: ["bookings"] });
    },
  });
}
