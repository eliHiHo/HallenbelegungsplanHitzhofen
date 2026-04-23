import { useState, type CSSProperties } from "react";
import { useQuery } from "@tanstack/react-query";
import { useCalendarWeek, getWeekStart } from "../features/calendar/useCalendar";
import { useHalls } from "../features/halls/useHalls";
import { bookingRequestsApi } from "../shared/api/bookingRequests";
import CalendarEntryDetail from "../features/calendar/CalendarEntryDetail";
import NewRequestModal from "../features/bookingRequests/NewRequestModal";
import { useAuth } from "../features/auth/AuthContext";
import type { CalendarEntry, BookingRequest, Hall } from "../shared/types/api";

// ── Formatierungs-Utilities ────────────────────────────────────────────────────

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

// ── Gruppierung ────────────────────────────────────────────────────────────────

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

// ── Hallenbezeichnungen ────────────────────────────────────────────────────────

// Leitet einen nutzerfreundlichen Anzeigenamen aus dem Hallentyp ab,
// damit die Legende konsistent zu den Spaltenköpfen "Klein" / "Groß" ist.
function hallDisplayName(hall: Hall): string {
  if (hall.type === "PART_SMALL") return "Kleine Halle";
  if (hall.type === "PART_LARGE") return "Große Halle";
  return hall.name; // Gesamthalle: DB-Namen behalten
}

// ── Spalten-Logik ──────────────────────────────────────────────────────────────

// 1 = Teilhalle A (PART_SMALL), 2 = Teilhalle B (PART_LARGE), 'full' = Gesamthalle
type Col = 1 | 2 | "full";

function getCol(hallId: string, hallMap: Record<string, Hall>): Col {
  const type = hallMap[hallId]?.type;
  if (type === "PART_SMALL") return 1;
  if (type === "PART_LARGE") return 2;
  return "full";
}

function colStyle(col: Col): CSSProperties {
  if (col === "full") return { gridColumn: "1 / span 2" };
  return { gridColumn: String(col) };
}

// ── Eintrag-Klasse ─────────────────────────────────────────────────────────────

function entryClass(entry: CalendarEntry): string {
  if (entry.status === "CANCELLED") return "calendar-entry--cancelled";
  if (entry.ownEntry) return "calendar-entry--own";
  if (entry.type === "BLOCKED_TIME") return "calendar-entry--blocked_time";
  return "calendar-entry--booking";
}

// ── Komponenten ────────────────────────────────────────────────────────────────

interface EntryButtonProps {
  entry: CalendarEntry;
  onClick: (e: CalendarEntry) => void;
  col: Col;
}

function EntryButton({ entry, onClick, col }: EntryButtonProps) {
  return (
    <button
      className={`calendar-entry ${entryClass(entry)}${col === 1 ? " cal-col-a" : ""}`}
      style={colStyle(col)}
      onClick={() => onClick(entry)}
    >
      <span className="entry-time">
        {timeLabel(entry.startDateTime)}–{timeLabel(entry.endDateTime)}
      </span>
      <span className="entry-title">{entry.title}</span>
      {/* Hallenname nur für Gesamthallen-Einträge — bei Teilhallen zeigt die Spalte es bereits */}
      {col === "full" && <span className="entry-hall">{entry.hallName}</span>}
    </button>
  );
}

function RequestChip({ req, col }: { req: BookingRequest; col: Col }) {
  return (
    <div
      className={`calendar-entry calendar-entry--request${col === 1 ? " cal-col-a" : ""}`}
      style={colStyle(col)}
    >
      <span className="entry-time">
        {timeLabel(req.startDateTime)}–{timeLabel(req.endDateTime)}
      </span>
      <span className="entry-title">{req.title}</span>
      {col === "full" && <span className="entry-hall">{req.hallName}</span>}
    </div>
  );
}

// ── Tages-Körper ───────────────────────────────────────────────────────────────

// Gemeinsamer Typ für sortierte Einträge eines Tages
interface ColItem {
  id: string;
  startDateTime: string;
  col: Col;
  kind: "entry" | "request";
  entry?: CalendarEntry;
  request?: BookingRequest;
}

interface DayBodyProps {
  entries: CalendarEntry[];
  requests: BookingRequest[];
  hallMap: Record<string, Hall>;
  hasSubColumns: boolean;
  onEntryClick: (e: CalendarEntry) => void;
}

function DayBody({ entries, requests, hallMap, hasSubColumns, onEntryClick }: DayBodyProps) {
  // Bug-Fix: Das Backend liefert ausstehende Buchungsanfragen sowohl als
  // CalendarEntry (ownEntry=true, grün) als auch als BookingRequest (gelb).
  // Deduplizierung: CalendarEntries, die einer pending Request entsprechen,
  // werden herausgefiltert — die gelbe Darstellung bleibt als einzige.
  const pendingKeys = new Set(
    requests.map((r) => `${r.hallId}|${r.startDateTime}`)
  );
  const dedupedEntries = entries.filter(
    (e) => !pendingKeys.has(`${e.hallId}|${e.startDateTime}`)
  );

  const allItems: ColItem[] = [
    ...dedupedEntries.map((e) => ({
      id: e.id,
      startDateTime: e.startDateTime,
      col: getCol(e.hallId, hallMap),
      kind: "entry" as const,
      entry: e,
    })),
    ...requests.map((r) => ({
      id: r.id,
      startDateTime: r.startDateTime,
      col: getCol(r.hallId, hallMap),
      kind: "request" as const,
      request: r,
    })),
  ].sort((a, b) => a.startDateTime.localeCompare(b.startDateTime));

  const isEmpty = allItems.length === 0;

  if (!hasSubColumns) {
    // Fallback: keine Teilhallen konfiguriert → einfache Liste
    return (
      <div className="calendar-day-single">
        {isEmpty ? (
          <div className="calendar-day-empty">–</div>
        ) : (
          allItems.map((item) =>
            item.kind === "entry" ? (
              <EntryButton key={item.id} entry={item.entry!} onClick={onEntryClick} col="full" />
            ) : (
              <RequestChip key={item.id} req={item.request!} col="full" />
            )
          )
        )}
      </div>
    );
  }

  // 2-Spalten-Grid: A | B
  // Einträge werden per CSS gridColumn platziert:
  //   col 1 ("full"): gridColumn 1 / span 2  → spannt über beide Spalten
  //   col 1 (PART_SMALL): gridColumn 1       → nur Spalte A
  //   col 2 (PART_LARGE): gridColumn 2       → nur Spalte B
  return (
    <div className="calendar-day-body">
      {/* Spalten-Köpfe — erste zwei Grid-Items, immer gleich breit */}
      <div className="cal-col-label cal-col-a">Klein</div>
      <div className="cal-col-label">Groß</div>

      {isEmpty ? (
        <div className="calendar-day-empty" style={{ gridColumn: "1 / span 2" }}>
          –
        </div>
      ) : (
        allItems.map((item) =>
          item.kind === "entry" ? (
            <EntryButton key={item.id} entry={item.entry!} onClick={onEntryClick} col={item.col} />
          ) : (
            <RequestChip key={item.id} req={item.request!} col={item.col} />
          )
        )
      )}
    </div>
  );
}

// ── Hauptkomponente ────────────────────────────────────────────────────────────

export default function CalendarPage() {
  const { user } = useAuth();
  const [weekStart, setWeekStart] = useState<string>(getWeekStart(new Date()));
  const [selectedEntry, setSelectedEntry] = useState<CalendarEntry | null>(null);
  const [showRequestForm, setShowRequestForm] = useState(false);
  const [requestInitialDate, setRequestInitialDate] = useState<string | undefined>(undefined);

  const { data: calendarWeek, isLoading: calLoading, error: calError } =
    useCalendarWeek(weekStart);
  const { data: halls, isLoading: hallsLoading } = useHalls();

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

  const weekDates: string[] = [];
  if (calendarWeek) {
    const start = new Date(calendarWeek.weekStart);
    for (let i = 0; i < 7; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      weekDates.push(d.toISOString().split("T")[0]);
    }
  }

  const groupedRequests =
    isClubRep && allRequests ? groupRequestsByDate(allRequests, weekDates) : {};

  // Prüfen ob Teilhallen konfiguriert sind (bestimmt ob 2-Spalten-Layout aktiv)
  const hasSubColumns = halls
    ? halls.some((h) => h.active && (h.type === "PART_SMALL" || h.type === "PART_LARGE"))
    : false;

  return (
    <div className="calendar-page">
      {/* Navigation */}
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

      {/* Farblegende */}
      <div className="calendar-legend">
        <span className="calendar-legend-item calendar-legend-item--booking">Buchungen</span>
        <span className="calendar-legend-item calendar-legend-item--own">Eigene Termine</span>
        {isClubRep && (
          <span className="calendar-legend-item calendar-legend-item--request">
            Anfragen (ausstehend)
          </span>
        )}
        <span className="calendar-legend-item calendar-legend-item--cancelled">Storniert</span>
        <span className="calendar-legend-item calendar-legend-item--blocked">Sperrzeiten</span>
      </div>

      {halls && halls.length > 0 && (
        <div className="hall-legend">
          {halls.map((h) => (
            <span key={h.id} className={`hall-badge hall-badge--${h.type.toLowerCase()}`}>
              {hallDisplayName(h)}
            </span>
          ))}
        </div>
      )}

      {/* Wochengitter */}
      <div className="calendar-week-grid">
        {weekDates.map((date) => (
          <div key={date} className="calendar-day-column">
            <div className="calendar-day-header">{isoToDisplay(date)}</div>
            <DayBody
              entries={grouped[date] ?? []}
              requests={groupedRequests[date] ?? []}
              hallMap={hallMap}
              hasSubColumns={hasSubColumns}
              onEntryClick={setSelectedEntry}
            />
          </div>
        ))}
      </div>

      {selectedEntry && (
        <CalendarEntryDetail
          entry={selectedEntry}
          onClose={() => setSelectedEntry(null)}
        />
      )}

      {showRequestForm && (
        <NewRequestModal
          initialDate={requestInitialDate}
          onClose={() => setShowRequestForm(false)}
          onSuccess={() => setShowRequestForm(false)}
        />
      )}
    </div>
  );
}
