import { api } from "./client";
import type { CalendarWeek, CalendarDay } from "../types/api";

export const calendarApi = {
  // weekStart: ISO date string "YYYY-MM-DD", optional (defaults to current week on backend)
  week: (weekStart?: string) => {
    const qs = weekStart ? `?weekStart=${weekStart}` : "";
    return api.get<CalendarWeek>(`/calendar/week${qs}`);
  },

  // day: ISO date string "YYYY-MM-DD", optional (defaults to today on backend)
  day: (day?: string) => {
    const qs = day ? `?day=${day}` : "";
    return api.get<CalendarDay>(`/calendar/day${qs}`);
  },
};
