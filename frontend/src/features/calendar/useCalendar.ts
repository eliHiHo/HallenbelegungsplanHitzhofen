import { useQuery } from "@tanstack/react-query";
import { calendarApi } from "../../shared/api/calendar";

// Returns ISO date string for the Monday of the week containing `date`
export function getWeekStart(date: Date): string {
  const d = new Date(date);
  const day = d.getDay(); // 0=Sun, 1=Mon, …
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d.toISOString().split("T")[0];
}

export function useCalendarWeek(weekStart?: string) {
  return useQuery({
    queryKey: ["calendar", "week", weekStart ?? "current"],
    queryFn: () => calendarApi.week(weekStart),
  });
}

export function useCalendarDay(day?: string) {
  return useQuery({
    queryKey: ["calendar", "day", day ?? "today"],
    queryFn: () => calendarApi.day(day),
  });
}
