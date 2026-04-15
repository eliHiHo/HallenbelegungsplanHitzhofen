import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";
import { useCancelBooking } from "./useCancelBooking";
import { ApiError } from "../../shared/api/client";

interface Props {
  bookingId: string;
  bookingTitle: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function CancelBookingModal({
  bookingId,
  bookingTitle,
  onClose,
  onSuccess,
}: Props) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Fetch full booking to verify canCancel flag from backend
  const { data: booking, isLoading } = useQuery({
    queryKey: ["bookings", bookingId],
    queryFn: () => bookingsApi.get(bookingId),
  });

  const { mutateAsync: cancelBooking, isPending } = useCancelBooking();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await cancelBooking({ id: bookingId, reason: reason.trim() || undefined });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("Stornierung nicht erlaubt.");
      } else {
        setError("Fehler beim Stornieren. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Buchung stornieren</h2>
        <p>
          <strong>{bookingTitle}</strong>
        </p>

        {isLoading && <p>Wird geladen…</p>}

        {!isLoading && booking && !booking.canCancel && (
          <p className="error">Diese Buchung kann nicht mehr storniert werden.</p>
        )}

        {!isLoading && booking && booking.canCancel && (
          <form onSubmit={handleSubmit} className="request-form">
            <div className="form-field">
              <label htmlFor="cancel-reason">
                Grund (optional)
              </label>
              <textarea
                id="cancel-reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
                placeholder="z. B. Veranstaltung entfällt"
              />
            </div>

            {error && <p className="error">{error}</p>}

            <div className="form-actions">
              <button type="button" onClick={onClose} disabled={isPending}>
                Abbrechen
              </button>
              <button type="submit" className="btn-reject" disabled={isPending}>
                {isPending ? "Wird storniert…" : "Jetzt stornieren"}
              </button>
            </div>
          </form>
        )}

        {/* TODO: full series cancellation requires bookingSeriesId in BookingDTO or CalendarEntry.
            Neither currently exposes it. When the backend adds that field, add
            "Gesamte Serie stornieren" here using DELETE /booking-series/{seriesId}. */}
      </div>
    </div>
  );
}
