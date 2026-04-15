import { api } from "./client";
import type { Hall } from "../types/api";

export const hallsApi = {
  list: (includeInactive?: boolean) => {
    const qs = includeInactive ? "?includeInactive=true" : "";
    return api.get<Hall[]>(`/halls${qs}`);
  },

  get: (hallId: string) =>
    api.get<Hall>(`/halls/${hallId}`),
};
