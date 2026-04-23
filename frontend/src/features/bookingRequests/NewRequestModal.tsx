import { useState, type FormEvent } from "react";
import { useHalls } from "../halls/useHalls";
import { useCreateBookingRequest } from "./useBookingRequests";
import { useCreateBookingSeriesRequest } from "../bookingSeriesRequests/useBookingSeriesRequests";
import { ApiError } from "../../shared/api/client";
import { DAY_OF_WEEK_OPTIONS, toLocalTime } from "../../shared/lib/dayOfWeek";
import type { DayOfWeek } from "../../shared/types/api";

type Mode = "single" | "series";

interface Props {
  initialDate?: string; // "YYYY-MM-DD" – vorausgefülltes Datum für Einzeltermine
  onClose: () => void;
  onSuccess: (message: string) => void;
}

// ── Helpers ────────────────────────────────────────────────────────────────────

function toLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value;
}

function defaultDateTimeFor(date: string, hour: number): string {
  return `${date}T${String(hour).padStart(2, "0")}:00`;
}

// ── Hauptkomponente ────────────────────────────────────────────────────────────

export default function NewRequestModal({ initialDate, onClose, onSuccess }: Props) {
  const [mode, setMode] = useState<Mode>("single");

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Neue Anfrage</h2>

        {/* Typ-Umschalter */}
        <div className="req-mode-toggle">
          <button
            type="button"
            className={`req-mode-btn${mode === "single" ? " req-mode-btn--active" : ""}`}
            onClick={() => setMode("single")}
          >
            Einzeltermin
          </button>
          <button
            type="button"
            className={`req-mode-btn${mode === "series" ? " req-mode-btn--active" : ""}`}
            onClick={() => setMode("series")}
          >
            Serientermin
          </button>
        </div>

        {mode === "single" ? (
          <SingleForm initialDate={initialDate} onClose={onClose} onSuccess={onSuccess} />
        ) : (
          <SeriesForm onClose={onClose} onSuccess={onSuccess} />
        )}
      </div>
    </div>
  );
}

// ── Einzeltermin-Formular ──────────────────────────────────────────────────────

function SingleForm({
  initialDate,
  onClose,
  onSuccess,
}: {
  initialDate?: string;
  onClose: () => void;
  onSuccess: (msg: string) => void;
}) {
  const today = initialDate ?? new Date().toISOString().split("T")[0];

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [hallId, setHallId] = useState("");
  const [startDateTime, setStartDateTime] = useState(defaultDateTimeFor(today, 9));
  const [endDateTime, setEndDateTime] = useState(defaultDateTimeFor(today, 10));
  const [error, setError] = useState<string | null>(null);

  const { data: halls } = useHalls();
  const { mutateAsync: createRequest, isPending } = useCreateBookingRequest();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!hallId) { setError("Bitte eine Halle auswählen."); return; }
    try {
      await createRequest({
        title, description, hallId,
        startDateTime: toLocalDateTime(startDateTime),
        endDateTime: toLocalDateTime(endDateTime),
      });
      onSuccess("Anfrage wurde erfolgreich gesendet.");
    } catch (err) {
      if (err instanceof ApiError && err.status === 409)
        setError("Der gewünschte Zeitraum ist bereits belegt oder blockiert.");
      else if (err instanceof ApiError && err.status === 400)
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      else
        setError("Fehler beim Senden. Bitte erneut versuchen.");
    }
  }

  return (
    <form onSubmit={handleSubmit} className="request-form">
      <div className="form-field">
        <label htmlFor="req-title">Titel</label>
        <input id="req-title" type="text" value={title}
          onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
      </div>

      <div className="form-field">
        <label htmlFor="req-hall">Halle</label>
        <select id="req-hall" value={hallId}
          onChange={(e) => setHallId(e.target.value)} required>
          <option value="">Bitte wählen…</option>
          {halls?.map((h) => (
            <option key={h.id} value={h.id}>{h.name}</option>
          ))}
        </select>
      </div>

      <div className="form-row">
        <div className="form-field">
          <label htmlFor="req-start">Von</label>
          <input id="req-start" type="datetime-local" value={startDateTime}
            onChange={(e) => setStartDateTime(e.target.value)} required />
        </div>
        <div className="form-field">
          <label htmlFor="req-end">Bis</label>
          <input id="req-end" type="datetime-local" value={endDateTime}
            onChange={(e) => setEndDateTime(e.target.value)} required />
        </div>
      </div>

      <div className="form-field">
        <label htmlFor="req-description">Beschreibung (optional)</label>
        <textarea id="req-description" value={description}
          onChange={(e) => setDescription(e.target.value)} rows={3} />
      </div>

      {error && <p className="error">{error}</p>}

      <div className="form-actions">
        <button type="button" onClick={onClose}>Abbrechen</button>
        <button type="submit" disabled={isPending}>
          {isPending ? "Wird gesendet…" : "Anfrage senden"}
        </button>
      </div>
    </form>
  );
}

// ── Serientermin-Formular ──────────────────────────────────────────────────────

function SeriesForm({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: (msg: string) => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [hallId, setHallId] = useState("");
  const [weekday, setWeekday] = useState<DayOfWeek>("MONDAY");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("10:00");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState<string | null>(null);

  const { data: halls } = useHalls();
  const { mutateAsync: createRequest, isPending } = useCreateBookingSeriesRequest();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!hallId) { setError("Bitte eine Halle auswählen."); return; }
    try {
      await createRequest({
        title, description, weekday, hallId,
        startTime: toLocalTime(startTime),
        endTime: toLocalTime(endTime),
        startDate, endDate,
      });
      onSuccess("Serienanfrage wurde erfolgreich gesendet.");
    } catch (err) {
      if (err instanceof ApiError && err.status === 409)
        setError("Konflikt: Der Zeitraum überschneidet sich mit bestehenden Einträgen.");
      else if (err instanceof ApiError && err.status === 400)
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      else
        setError("Fehler beim Senden. Bitte erneut versuchen.");
    }
  }

  return (
    <form onSubmit={handleSubmit} className="request-form">
      <div className="form-field">
        <label htmlFor="sr-title">Titel</label>
        <input id="sr-title" type="text" value={title}
          onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
      </div>

      <div className="form-field">
        <label htmlFor="sr-hall">Halle</label>
        <select id="sr-hall" value={hallId}
          onChange={(e) => setHallId(e.target.value)} required>
          <option value="">Bitte wählen…</option>
          {halls?.map((h) => (
            <option key={h.id} value={h.id}>{h.name}</option>
          ))}
        </select>
      </div>

      <div className="form-field">
        <label htmlFor="sr-weekday">Wochentag</label>
        <select id="sr-weekday" value={weekday}
          onChange={(e) => setWeekday(e.target.value as DayOfWeek)} required>
          {DAY_OF_WEEK_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </div>

      <div className="form-row">
        <div className="form-field">
          <label htmlFor="sr-start-time">Von (Uhrzeit)</label>
          <input id="sr-start-time" type="time" value={startTime}
            onChange={(e) => setStartTime(e.target.value)} required />
        </div>
        <div className="form-field">
          <label htmlFor="sr-end-time">Bis (Uhrzeit)</label>
          <input id="sr-end-time" type="time" value={endTime}
            onChange={(e) => setEndTime(e.target.value)} required />
        </div>
      </div>

      <div className="form-row">
        <div className="form-field">
          <label htmlFor="sr-start-date">Startdatum</label>
          <input id="sr-start-date" type="date" value={startDate}
            onChange={(e) => setStartDate(e.target.value)} required />
        </div>
        <div className="form-field">
          <label htmlFor="sr-end-date">Enddatum</label>
          <input id="sr-end-date" type="date" value={endDate}
            onChange={(e) => setEndDate(e.target.value)} required />
        </div>
      </div>

      <div className="form-field">
        <label htmlFor="sr-description">Beschreibung (optional)</label>
        <textarea id="sr-description" value={description}
          onChange={(e) => setDescription(e.target.value)} rows={3} />
      </div>

      {error && <p className="error">{error}</p>}

      <div className="form-actions">
        <button type="button" onClick={onClose}>Abbrechen</button>
        <button type="submit" disabled={isPending}>
          {isPending ? "Wird gesendet…" : "Anfrage senden"}
        </button>
      </div>
    </form>
  );
}
