import type { DayOfWeek } from "../types/api";

export const DAY_OF_WEEK_OPTIONS: { value: DayOfWeek; label: string }[] = [
  { value: "MONDAY", label: "Montag" },
  { value: "TUESDAY", label: "Dienstag" },
  { value: "WEDNESDAY", label: "Mittwoch" },
  { value: "THURSDAY", label: "Donnerstag" },
  { value: "FRIDAY", label: "Freitag" },
  { value: "SATURDAY", label: "Samstag" },
  { value: "SUNDAY", label: "Sonntag" },
];

export const DAY_OF_WEEK_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "Montag",
  TUESDAY: "Dienstag",
  WEDNESDAY: "Mittwoch",
  THURSDAY: "Donnerstag",
  FRIDAY: "Freitag",
  SATURDAY: "Samstag",
  SUNDAY: "Sonntag",
};

// Convert <input type="time"> value "HH:mm" to "HH:mm:ss" for backend
export function toLocalTime(value: string): string {
  return value.length === 5 ? `${value}:00` : value;
}

// Format "HH:mm:ss" to display "HH:mm"
export function formatTime(iso: string): string {
  return iso.slice(0, 5);
}
