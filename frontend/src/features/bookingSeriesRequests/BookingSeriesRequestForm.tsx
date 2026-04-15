import { useState, type FormEvent } from "react";
import { useHalls } from "../halls/useHalls";
import { useCreateBookingSeriesRequest } from "./useBookingSeriesRequests";
import { ApiError } from "../../shared/api/client";
import {
  DAY_OF_WEEK_OPTIONS,
  toLocalTime,
} from "../../shared/lib/dayOfWeek";
import type { DayOfWeek } from "../../shared/types/api";

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export default function BookingSeriesRequestForm({ onClose, onSuccess }: Props) {
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

    if (!hallId) {
      setError("Bitte eine Halle auswählen.");
      return;
    }

    try {
      await createRequest({
        title,
        description,
        weekday,
        startTime: toLocalTime(startTime),
        endTime: toLocalTime(endTime),
        startDate,
        endDate,
        hallId,
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Konflikt: Der Zeitraum überschneidet sich mit bestehenden Einträgen.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      } else {
        setError("Fehler beim Senden. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Neue Serienanfrage</h2>

        <form onSubmit={handleSubmit} className="request-form">
          <div className="form-field">
            <label htmlFor="sr-title">Titel</label>
            <input
              id="sr-title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              maxLength={200}
            />
          </div>

          <div className="form-field">
            <label htmlFor="sr-hall">Halle</label>
            <select
              id="sr-hall"
              value={hallId}
              onChange={(e) => setHallId(e.target.value)}
              required
            >
              <option value="">Bitte wählen…</option>
              {halls?.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="sr-weekday">Wochentag</label>
            <select
              id="sr-weekday"
              value={weekday}
              onChange={(e) => setWeekday(e.target.value as DayOfWeek)}
              required
            >
              {DAY_OF_WEEK_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label htmlFor="sr-start-time">Von (Uhrzeit)</label>
              <input
                id="sr-start-time"
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="sr-end-time">Bis (Uhrzeit)</label>
              <input
                id="sr-end-time"
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label htmlFor="sr-start-date">Startdatum</label>
              <input
                id="sr-start-date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="sr-end-date">Enddatum</label>
              <input
                id="sr-end-date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-field">
            <label htmlFor="sr-description">Beschreibung (optional)</label>
            <textarea
              id="sr-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
            />
          </div>

          {error && <p className="error">{error}</p>}

          <div className="form-actions">
            <button type="button" onClick={onClose}>Abbrechen</button>
            <button type="submit" disabled={isPending}>
              {isPending ? "Wird gesendet…" : "Anfrage senden"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
