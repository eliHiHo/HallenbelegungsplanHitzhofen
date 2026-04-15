import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { blockedTimesApi } from "../../shared/api/blockedTimes";
import type { BlockedTimeCreate } from "../../shared/types/api";

export const BLOCKED_TIMES_KEY = ["blocked-times"];

export function useBlockedTimes() {
  return useQuery({
    queryKey: BLOCKED_TIMES_KEY,
    queryFn: blockedTimesApi.list,
  });
}

export function useCreateBlockedTime() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BlockedTimeCreate) => blockedTimesApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: BLOCKED_TIMES_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

export function useDeleteBlockedTime() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => blockedTimesApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: BLOCKED_TIMES_KEY });
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}
