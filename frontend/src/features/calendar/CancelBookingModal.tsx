import { useState, type FormEvent } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingsApi } from "../../shared/api/bookings";
import { bookingSeriesApi } from "../../shared/api/bookingSeries";
import { useCancelBooking } from "./useCancelBooking";
import { ApiError } from "../../shared/api/client";

interface Props {
  bookingId: string;
  bookingTitle: string;
  bookingSeriesId?: string | null;
  onClose: () => void;
  onSuccess: () => void;
}

export default function CancelBookingModal({
  bookingId,
  bookingTitle,
  bookingSeriesId,
  onClose,
  onSuccess,
}: Props) {
  const [reason, setReason] = useState("");
  const [cancelMode, setCancelMode] = useState<"single" | "series">("single");
  const [error, setError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  // Fetch full booking to verify canCancel flag from backend
  const { data: booking, isLoading } = useQuery({
    queryKey: ["bookings", bookingId],
    queryFn: () => bookingsApi.get(bookingId),
  });

  const { mutateAsync: cancelBooking, isPending: cancelPending } = useCancelBooking();

  const { mutateAsync: cancelSeries, isPending: seriesPending } = useMutation({
    mutationFn: ({ seriesId, r }: { seriesId: string; r?: string }) =>
      bookingSeriesApi.cancelSeries(seriesId, r),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });

  const isPending = cancelPending || seriesPending;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const trimmedReason = reason.trim() || undefined;

    try {
      if (cancelMode === "series" && bookingSeriesId) {
        await cancelSeries({ seriesId: bookingSeriesId, r: trimmedReason });
      } else {
        await cancelBooking({ id: bookingId, reason: trimmedReason });
      }
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
            {bookingSeriesId && (
              <div className="form-field">
                <label>Was soll storniert werden?</label>
                <div style={{ display: "flex", flexDirection: "column", gap: "0.4rem", marginTop: "0.25rem" }}>
                  <label style={{ fontWeight: "normal", display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <input
                      type="radio"
                      name="cancelMode"
                      value="single"
                      checked={cancelMode === "single"}
                      onChange={() => setCancelMode("single")}
                    />
                    Nur diesen Termin
                  </label>
                  <label style={{ fontWeight: "normal", display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <input
                      type="radio"
                      name="cancelMode"
                      value="series"
                      checked={cancelMode === "series"}
                      onChange={() => setCancelMode("series")}
                    />
                    Gesamte Serie stornieren
                  </label>
                </div>
              </div>
            )}

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
                {isPending
                  ? "Wird storniert…"
                  : cancelMode === "series"
                  ? "Gesamte Serie stornieren"
                  : "Jetzt stornieren"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
