import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingSeriesApi } from "../../shared/api/bookingSeries";

export function useCancelBookingSeries() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ seriesId, reason }: { seriesId: string; reason?: string }) =>
      bookingSeriesApi.cancelSeries(seriesId, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
      void queryClient.invalidateQueries({ queryKey: ["statistics"] });
    },
  });
}

export function useCancelSeriesOccurrence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      seriesId,
      bookingId,
      reason,
    }: {
      seriesId: string;
      bookingId: string;
      reason?: string;
    }) => bookingSeriesApi.cancelOccurrence(seriesId, bookingId, reason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
      void queryClient.invalidateQueries({ queryKey: ["statistics"] });
    },
  });
}
