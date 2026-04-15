import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";
import { useFeedbackBooking } from "./useFeedbackBooking";
import { ApiError } from "../../shared/api/client";

interface Props {
  bookingId: string;
  bookingTitle: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function FeedbackModal({
  bookingId,
  bookingTitle,
  onClose,
  onSuccess,
}: Props) {
  const [error, setError] = useState<string | null>(null);

  // Fetch full booking to get canViewFeedback and pre-populate existing values
  const { data: booking, isLoading } = useQuery({
    queryKey: ["bookings", bookingId],
    queryFn: () => bookingsApi.get(bookingId),
  });

  // Initialise form from existing feedback once booking loads
  const [participantCount, setParticipantCount] = useState<string>("");
  const [comment, setComment] = useState<string>("");
  const [initialised, setInitialised] = useState(false);

  if (booking && !initialised) {
    setParticipantCount(booking.participantCount != null ? String(booking.participantCount) : "");
    setComment(booking.feedbackComment ?? "");
    setInitialised(true);
  }

  const { mutateAsync: submitFeedback, isPending } = useFeedbackBooking();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const count = participantCount.trim() !== "" ? parseInt(participantCount, 10) : null;

    try {
      await submitFeedback({
        id: bookingId,
        payload: {
          participantCount: count,
          comment: comment.trim() || null,
        },
      });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte Teilnehmerzahl prüfen.");
      } else if (err instanceof ApiError && err.status === 403) {
        setError("Kein Zugriff auf dieses Feedback.");
      } else {
        setError("Fehler beim Speichern. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Feedback eintragen</h2>
        <p>
          <strong>{bookingTitle}</strong>
        </p>

        {isLoading && <p>Wird geladen…</p>}

        {!isLoading && booking && !booking.canViewFeedback && (
          <p className="error">Feedback kann für diese Buchung nicht eingetragen werden.</p>
        )}

        {!isLoading && booking && booking.canViewFeedback && (
          <form onSubmit={handleSubmit} className="request-form">
            <div className="form-field">
              <label htmlFor="feedback-count">Teilnehmeranzahl</label>
              <input
                id="feedback-count"
                type="number"
                min={0}
                value={participantCount}
                onChange={(e) => setParticipantCount(e.target.value)}
                placeholder="z. B. 12"
              />
            </div>

            <div className="form-field">
              <label htmlFor="feedback-comment">Kommentar (optional)</label>
              <textarea
                id="feedback-comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={3}
                placeholder="z. B. Gute Beteiligung"
              />
            </div>

            {error && <p className="error">{error}</p>}

            <div className="form-actions">
              <button type="button" onClick={onClose} disabled={isPending}>
                Abbrechen
              </button>
              <button type="submit" disabled={isPending}>
                {isPending ? "Wird gespeichert…" : "Feedback speichern"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
