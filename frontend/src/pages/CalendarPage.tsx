import { useState } from "react";
import { useCalendarWeek, getWeekStart } from "../features/calendar/useCalendar";
import { useHalls } from "../features/halls/useHalls";
import CalendarEntryDetail from "../features/calendar/CalendarEntryDetail";
import BookingRequestForm from "../features/bookingRequests/BookingRequestForm";
import { useAuth } from "../features/auth/AuthContext";
import type { CalendarEntry, Hall } from "../shared/types/api";

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
          return (
            <div key={date} className="calendar-day-column">
              <div className="calendar-day-header">{isoToDisplay(date)}</div>
              {entries.length === 0 ? (
                <div className="calendar-day-empty">–</div>
              ) : (
                entries.map((entry) => (
                  <button
                    key={entry.id}
                    className={`calendar-entry calendar-entry--${entry.type.toLowerCase()}`}
                    onClick={() => setSelectedEntry(entry)}
                  >
                    <span className="entry-time">
                      {timeLabel(entry.startDateTime)}–{timeLabel(entry.endDateTime)}
                    </span>
                    <span className="entry-title">{entry.title}</span>
                    <span className="entry-hall">{entry.hallName}</span>
                  </button>
                ))
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
