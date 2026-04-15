import { useState, type FormEvent } from "react";
import { useHalls } from "../halls/useHalls";
import { useCreateBlockedTime } from "./useBlockedTimes";
import { ApiError } from "../../shared/api/client";

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

// Convert datetime-local input "YYYY-MM-DDTHH:mm" → "YYYY-MM-DDTHH:mm:ss"
function toLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value;
}

export default function BlockedTimeForm({ onClose, onSuccess }: Props) {
  const [hallId, setHallId] = useState("");
  const [reason, setReason] = useState("");
  const [startDateTime, setStartDateTime] = useState("");
  const [endDateTime, setEndDateTime] = useState("");
  const [error, setError] = useState<string | null>(null);

  const { data: halls } = useHalls(true); // include inactive so admin sees all
  const { mutateAsync: create, isPending } = useCreateBlockedTime();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!hallId) {
      setError("Bitte eine Halle auswählen.");
      return;
    }

    try {
      await create({
        hallId,
        reason: reason.trim() || undefined,
        startDateTime: toLocalDateTime(startDateTime),
        endDateTime: toLocalDateTime(endDateTime),
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Zeitraum überschneidet sich mit einer bestehenden Sperrzeit.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      } else {
        setError("Fehler beim Speichern. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Neue Sperrzeit</h2>

        <form onSubmit={handleSubmit} className="request-form">
          <div className="form-field">
            <label htmlFor="bt-hall">Halle</label>
            <select
              id="bt-hall"
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

          <div className="form-row">
            <div className="form-field">
              <label htmlFor="bt-start">Von</label>
              <input
                id="bt-start"
                type="datetime-local"
                value={startDateTime}
                onChange={(e) => setStartDateTime(e.target.value)}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="bt-end">Bis</label>
              <input
                id="bt-end"
                type="datetime-local"
                value={endDateTime}
                onChange={(e) => setEndDateTime(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-field">
            <label htmlFor="bt-reason">Grund (optional)</label>
            <input
              id="bt-reason"
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
              placeholder="z. B. Hallenpflege, Turnier"
            />
          </div>

          {error && <p className="error">{error}</p>}

          <div className="form-actions">
            <button type="button" onClick={onClose}>Abbrechen</button>
            <button type="submit" disabled={isPending}>
              {isPending ? "Wird gespeichert…" : "Sperrzeit anlegen"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
