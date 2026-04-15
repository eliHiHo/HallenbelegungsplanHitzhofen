import { useState, type FormEvent } from "react";
import { useHalls } from "../halls/useHalls";
import { useCreateBookingRequest } from "./useBookingRequests";
import { ApiError } from "../../shared/api/client";

interface Props {
  // Optional prefill values (e.g. from clicking a calendar date)
  initialDate?: string; // "YYYY-MM-DD"
  onClose: () => void;
  onSuccess: () => void;
}

// Convert datetime-local input value ("YYYY-MM-DDTHH:mm") to LocalDateTime string
function toLocalDateTime(value: string): string {
  // Backend expects "YYYY-MM-DDTHH:mm:ss" (ISO LocalDateTime, no timezone)
  return value.length === 16 ? `${value}:00` : value;
}

function defaultDateTimeFor(date: string, hour: number): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date}T${pad(hour)}:00`;
}

export default function BookingRequestForm({ initialDate, onClose, onSuccess }: Props) {
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

    if (!hallId) {
      setError("Bitte eine Halle auswählen.");
      return;
    }

    try {
      await createRequest({
        title,
        description,
        hallId,
        startDateTime: toLocalDateTime(startDateTime),
        endDateTime: toLocalDateTime(endDateTime),
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Der gewünschte Zeitraum ist bereits belegt oder blockiert.");
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
        <h2>Neue Buchungsanfrage</h2>

        <form onSubmit={handleSubmit} className="request-form">
          <div className="form-field">
            <label htmlFor="req-title">Titel</label>
            <input
              id="req-title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              maxLength={200}
            />
          </div>

          <div className="form-field">
            <label htmlFor="req-hall">Halle</label>
            <select
              id="req-hall"
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
            <label htmlFor="req-start">Von</label>
            <input
              id="req-start"
              type="datetime-local"
              value={startDateTime}
              onChange={(e) => setStartDateTime(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="req-end">Bis</label>
            <input
              id="req-end"
              type="datetime-local"
              value={endDateTime}
              onChange={(e) => setEndDateTime(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="req-description">Beschreibung (optional)</label>
            <textarea
              id="req-description"
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
