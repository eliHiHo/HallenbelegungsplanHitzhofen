import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useCalendarWeek, getWeekStart } from "../features/calendar/useCalendar";
import { useHalls } from "../features/halls/useHalls";
import { bookingRequestsApi } from "../shared/api/bookingRequests";
import CalendarEntryDetail from "../features/calendar/CalendarEntryDetail";
import BookingRequestForm from "../features/bookingRequests/BookingRequestForm";
import { useAuth } from "../features/auth/AuthContext";
import type { CalendarEntry, BookingRequest, Hall } from "../shared/types/api";

function isoToDisplay(iso: string): string {
  return new Date(iso).toLocaleDateString("de-DE", {
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
  });
}

function timeLabel(iso: string): string {
  return new Date(iso).toLocaleTimeString("de-DE", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

// Group entries by date string "YYYY-MM-DD"
function groupByDate(entries: CalendarEntry[]): Record<string, CalendarEntry[]> {
  const groups: Record<string, CalendarEntry[]> = {};
  for (const entry of entries) {
    const date = entry.startDateTime.split("T")[0];
    if (!groups[date]) groups[date] = [];
    groups[date].push(entry);
  }
  return groups;
}

function groupRequestsByDate(
  requests: BookingRequest[],
  weekDates: string[]
): Record<string, BookingRequest[]> {
  const dateSet = new Set(weekDates);
  const groups: Record<string, BookingRequest[]> = {};
  for (const req of requests) {
    if (req.status !== "PENDING") continue;
    const date = req.startDateTime.split("T")[0];
    if (!dateSet.has(date)) continue;
    if (!groups[date]) groups[date] = [];
    groups[date].push(req);
  }
  return groups;
}

export default function CalendarPage() {
  const { user } = useAuth();
  const [weekStart, setWeekStart] = useState<string>(
    getWeekStart(new Date())
  );
  const [selectedEntry, setSelectedEntry] = useState<CalendarEntry | null>(null);
  const [showRequestForm, setShowRequestForm] = useState(false);
  // Prefill date when club rep opens the form from the calendar nav area
  const [requestInitialDate, setRequestInitialDate] = useState<string | undefined>(undefined);

  const { data: calendarWeek, isLoading: calLoading, error: calError } =
    useCalendarWeek(weekStart);
  const { data: halls, isLoading: hallsLoading } = useHalls();

  // For club reps: fetch own pending requests to overlay in calendar
  const isClubRep = user?.role === "CLUB_REPRESENTATIVE";
  const { data: allRequests } = useQuery({
    queryKey: ["booking-requests"],
    queryFn: bookingRequestsApi.list,
    enabled: isClubRep,
  });

  function prevWeek() {
    const d = new Date(weekStart);
    d.setDate(d.getDate() - 7);
    setWeekStart(d.toISOString().split("T")[0]);
  }

  function nextWeek() {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + 7);
    setWeekStart(d.toISOString().split("T")[0]);
  }

  function goToToday() {
    setWeekStart(getWeekStart(new Date()));
  }

  if (calLoading || hallsLoading) {
    return <div className="calendar-loading">Wird geladen…</div>;
  }

  if (calError) {
    return (
      <div className="calendar-error">
        Fehler beim Laden des Kalenders. Bitte Seite neu laden.
      </div>
    );
  }

  const grouped = calendarWeek ? groupByDate(calendarWeek.entries) : {};
  const hallMap: Record<string, Hall> = {};
  if (halls) {
    for (const hall of halls) hallMap[hall.id] = hall;
  }

  // Build sorted list of dates for the week
  const weekDates: string[] = [];
  if (calendarWeek) {
    const start = new Date(calendarWeek.weekStart);
    for (let i = 0; i < 7; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      weekDates.push(d.toISOString().split("T")[0]);
    }
  }

  const groupedRequests = isClubRep && allRequests
    ? groupRequestsByDate(allRequests, weekDates)
    : {};

  return (
    <div className="calendar-page">
      <div className="calendar-nav">
        <button onClick={prevWeek}>← Vorherige</button>
        <button onClick={goToToday}>Heute</button>
        <button onClick={nextWeek}>Nächste →</button>
        {calendarWeek && (
          <span className="calendar-week-label">
            KW {weekStart} – {calendarWeek.weekEnd}
          </span>
        )}
        {user?.role === "CLUB_REPRESENTATIVE" && (
          <button
            className="btn-primary"
            onClick={() => {
              setRequestInitialDate(weekStart);
              setShowRequestForm(true);
            }}
          >
            + Neue Anfrage
          </button>
        )}
      </div>

      {/* Color legend */}
      <div className="calendar-legend">
        <span className="calendar-legend-item calendar-legend-item--booking">Buchungen</span>
        <span className="calendar-legend-item calendar-legend-item--own">Eigene Termine</span>
        {isClubRep && (
          <span className="calendar-legend-item calendar-legend-item--request">Anfragen (ausstehend)</span>
        )}
        <span className="calendar-legend-item calendar-legend-item--cancelled">Storniert</span>
        <span className="calendar-legend-item calendar-legend-item--blocked">Sperrzeiten</span>
      </div>

      {halls && halls.length > 0 && (
        <div className="hall-legend">
          {halls.map((h) => (
            <span key={h.id} className="hall-badge">
              {h.name}
            </span>
          ))}
        </div>
      )}

      <div className="calendar-week-grid">
        {weekDates.map((date) => {
          const entries = grouped[date] ?? [];
          const requests = groupedRequests[date] ?? [];
          const hasContent = entries.length > 0 || requests.length > 0;
          return (
            <div key={date} className="calendar-day-column">
              <div className="calendar-day-header">{isoToDisplay(date)}</div>
              {!hasContent ? (
                <div className="calendar-day-empty">–</div>
              ) : (
                <>
                  {entries.map((entry) => (
                    <button
                      key={entry.id}
                      className={[
                        "calendar-entry",
                        entry.status === "CANCELLED"
                          ? "calendar-entry--cancelled"
                          : entry.ownEntry
                          ? "calendar-entry--own"
                          : entry.type === "BLOCKED_TIME"
                          ? "calendar-entry--blocked_time"
                          : "calendar-entry--booking",
                      ].join(" ")}
                      onClick={() => setSelectedEntry(entry)}
                    >
                      <span className="entry-time">
                        {timeLabel(entry.startDateTime)}–{timeLabel(entry.endDateTime)}
                      </span>
                      <span className="entry-title">{entry.title}</span>
                      <span className="entry-hall">{entry.hallName}</span>
                    </button>
                  ))}
                  {requests.map((req) => (
                    <div key={req.id} className="calendar-entry calendar-entry--request">
                      <span className="entry-time">
                        {timeLabel(req.startDateTime)}–{timeLabel(req.endDateTime)}
                      </span>
                      <span className="entry-title">{req.title}</span>
                      <span className="entry-hall">{req.hallName}</span>
                    </div>
                  ))}
                </>
              )}
            </div>
          );
        })}
      </div>

      {selectedEntry && (
        <CalendarEntryDetail
          entry={selectedEntry}
          onClose={() => setSelectedEntry(null)}
        />
      )}

      {showRequestForm && (
        <BookingRequestForm
          initialDate={requestInitialDate}
          onClose={() => setShowRequestForm(false)}
          onSuccess={() => setShowRequestForm(false)}
        />
      )}
    </div>
  );
}
