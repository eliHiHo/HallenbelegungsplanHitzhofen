import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";
import { useUpdateBooking } from "./useUpdateBooking";
import { useHalls } from "../halls/useHalls";
import { ApiError } from "../../shared/api/client";

interface Props {
  bookingId: string;
  bookingTitle: string;
  onClose: () => void;
  onSuccess: () => void;
}

// "YYYY-MM-DDTHH:mm:ss" → "YYYY-MM-DDTHH:mm" for datetime-local input
function toInputValue(iso: string): string {
  return iso.length >= 16 ? iso.substring(0, 16) : iso;
}

// "YYYY-MM-DDTHH:mm" → "YYYY-MM-DDTHH:mm:ss" for backend
function toLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value;
}

export default function EditBookingModal({
  bookingId,
  bookingTitle,
  onClose,
  onSuccess,
}: Props) {
  const [error, setError] = useState<string | null>(null);

  // Fetch full booking to verify canEdit and pre-populate fields
  const { data: booking, isLoading: bookingLoading } = useQuery({
    queryKey: ["bookings", bookingId],
    queryFn: () => bookingsApi.get(bookingId),
  });

  const { data: halls } = useHalls();
  const { mutateAsync: updateBooking, isPending } = useUpdateBooking();

  // Controlled form state — initialised once booking loads
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [hallId, setHallId] = useState("");
  const [startDateTime, setStartDateTime] = useState("");
  const [endDateTime, setEndDateTime] = useState("");
  const [initialised, setInitialised] = useState(false);

  if (booking && !initialised) {
    setTitle(booking.title ?? "");
    setDescription(booking.description ?? "");
    setHallId(booking.hallId);
    setStartDateTime(toInputValue(booking.startDateTime));
    setEndDateTime(toInputValue(booking.endDateTime));
    setInitialised(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!hallId) {
      setError("Bitte eine Halle auswählen.");
      return;
    }

    try {
      await updateBooking({
        id: bookingId,
        data: {
          hallId,
          title,
          description,
          startDateTime: toLocalDateTime(startDateTime),
          endDateTime: toLocalDateTime(endDateTime),
        },
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Zeitraum ist bereits belegt oder blockiert.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      } else if (err instanceof ApiError && err.status === 403) {
        setError("Diese Buchung kann nicht bearbeitet werden.");
      } else {
        setError("Fehler beim Speichern. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Buchung bearbeiten</h2>
        <p>
          <strong>{bookingTitle}</strong>
        </p>

        {bookingLoading && <p>Wird geladen…</p>}

        {!bookingLoading && booking && !booking.canEdit && (
          <p className="error">Diese Buchung kann nicht bearbeitet werden.</p>
        )}

        {!bookingLoading && booking && booking.canEdit && (
          <form onSubmit={handleSubmit} className="request-form">
            <div className="form-field">
              <label htmlFor="edit-title">Titel</label>
              <input
                id="edit-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                maxLength={200}
              />
            </div>

            <div className="form-field">
              <label htmlFor="edit-hall">Halle</label>
              <select
                id="edit-hall"
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
              <label htmlFor="edit-start">Von</label>
              <input
                id="edit-start"
                type="datetime-local"
                value={startDateTime}
                onChange={(e) => setStartDateTime(e.target.value)}
                required
              />
            </div>

            <div className="form-field">
              <label htmlFor="edit-end">Bis</label>
              <input
                id="edit-end"
                type="datetime-local"
                value={endDateTime}
                onChange={(e) => setEndDateTime(e.target.value)}
                required
              />
            </div>

            <div className="form-field">
              <label htmlFor="edit-description">Beschreibung (optional)</label>
              <textarea
                id="edit-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
              />
            </div>

            {error && <p className="error">{error}</p>}

            <div className="form-actions">
              <button type="button" onClick={onClose} disabled={isPending}>
                Abbrechen
              </button>
              <button type="submit" disabled={isPending}>
                {isPending ? "Wird gespeichert…" : "Speichern"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
