import { api } from "./client";
import type { BlockedTime, BlockedTimeCreate } from "../types/api";

export const blockedTimesApi = {
  list: () =>
    api.get<BlockedTime[]>("/blocked-times"),

  create: (data: BlockedTimeCreate) =>
    // id and hallName are ignored by the backend; only hallId, reason,
    // startDateTime, endDateTime are used.
    api.post<void>("/blocked-times", {
      hallId: data.hallId,
      reason: data.reason ?? null,
      startDateTime: data.startDateTime,
      endDateTime: data.endDateTime,
    }),

  delete: (id: string) =>
    api.delete<void>(`/blocked-times/${id}`),
};
