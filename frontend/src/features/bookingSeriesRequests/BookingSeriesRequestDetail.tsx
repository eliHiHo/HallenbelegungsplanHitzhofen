import { useState, type FormEvent } from "react";
import {
  useApproveBookingSeriesRequest,
  useRejectBookingSeriesRequest,
} from "./useBookingSeriesRequests";
import { ApiError } from "../../shared/api/client";
import { DAY_OF_WEEK_LABELS, formatTime } from "../../shared/lib/dayOfWeek";
import type { BookingSeriesRequest, BookingSeriesApproveResult } from "../../shared/types/api";

interface Props {
  request: BookingSeriesRequest;
  onClose: () => void;
  onActionSuccess: (message: string) => void;
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Ausstehend",
  APPROVED: "Genehmigt",
  REJECTED: "Abgelehnt",
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export default function BookingSeriesRequestDetail({ request, onClose, onActionSuccess }: Props) {
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [approveResult, setApproveResult] = useState<BookingSeriesApproveResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const approve = useApproveBookingSeriesRequest();
  const reject = useRejectBookingSeriesRequest();

  const isPending = request.status === "PENDING";
  const isActing = approve.isPending || reject.isPending;

  async function handleApprove() {
    setError(null);
    try {
      const result = await approve.mutateAsync(request.id);
      setApproveResult(result);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Fehler beim Genehmigen. Bitte erneut versuchen.");
      }
    }
  }

  async function handleReject(e: FormEvent) {
    e.preventDefault();
    if (!rejectionReason.trim()) return;
    setError(null);
    try {
      await reject.mutateAsync({ id: request.id, reason: rejectionReason.trim() });
      onActionSuccess("Serienanfrage wurde abgelehnt.");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Fehler beim Ablehnen. Bitte erneut versuchen.");
      }
    }
  }

  // After approve: show result summary before closing
  if (approveResult) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
          <button className="modal-close" onClick={onClose}>✕</button>
          <h2>Serienanfrage genehmigt</h2>
          <p>
            <strong>{approveResult.createdBookingIds.length}</strong> Buchung(en) wurden erstellt.
          </p>
          {approveResult.skippedOccurrences.length > 0 && (
            <>
              <p>
                <strong>{approveResult.skippedOccurrences.length}</strong> Termin(e) wurden
                übersprungen (Konflikt oder Feiertag):
              </p>
              <ul className="skipped-list">
                {approveResult.skippedOccurrences.map((d) => (
                  <li key={d}>{formatDate(d)}</li>
                ))}
              </ul>
            </>
          )}
          <div className="detail-actions">
            <button
              className="btn-primary"
              onClick={() => {
                onActionSuccess(
                  `Genehmigt: ${approveResult.createdBookingIds.length} Buchung(en) erstellt.`
                );
                onClose();
              }}
            >
              Schließen
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>{request.title}</h2>

        <dl className="detail-list">
          <dt>Halle</dt>
          <dd>{request.hallName}</dd>

          <dt>Anfragender</dt>
          <dd>{request.requestedByName}</dd>

          <dt>Wochentag</dt>
          <dd>{DAY_OF_WEEK_LABELS[request.weekday]}</dd>

          <dt>Uhrzeit</dt>
          <dd>{formatTime(request.startTime)} – {formatTime(request.endTime)}</dd>

          <dt>Zeitraum</dt>
          <dd>{formatDate(request.startDate)} – {formatDate(request.endDate)}</dd>

          <dt>Status</dt>
          <dd>
            <span className={`status-badge status-badge--${request.status.toLowerCase()}`}>
              {STATUS_LABELS[request.status] ?? request.status}
            </span>
          </dd>

          {request.description && (
            <>
              <dt>Beschreibung</dt>
              <dd>{request.description}</dd>
            </>
          )}

          {request.rejectionReason && (
            <>
              <dt>Ablehnungsgrund</dt>
              <dd className="rejection-reason">{request.rejectionReason}</dd>
            </>
          )}
        </dl>

        {error && <p className="error" style={{ marginTop: "0.75rem" }}>{error}</p>}

        {isPending && !showRejectForm && (
          <div className="detail-actions">
            <button
              className="btn-approve"
              onClick={handleApprove}
              disabled={isActing}
            >
              {approve.isPending ? "Wird genehmigt…" : "Genehmigen"}
            </button>
            <button
              className="btn-reject"
              onClick={() => setShowRejectForm(true)}
              disabled={isActing}
            >
              Ablehnen
            </button>
          </div>
        )}

        {isPending && showRejectForm && (
          <form onSubmit={handleReject} className="reject-form">
            <div className="form-field">
              <label htmlFor="sr-reject-reason">Ablehnungsgrund</label>
              <textarea
                id="sr-reject-reason"
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
                required
                autoFocus
              />
            </div>
            <div className="form-actions">
              <button
                type="button"
                onClick={() => { setShowRejectForm(false); setRejectionReason(""); }}
                disabled={isActing}
              >
                Abbrechen
              </button>
              <button
                type="submit"
                className="btn-reject"
                disabled={isActing || !rejectionReason.trim()}
              >
                {reject.isPending ? "Wird abgelehnt…" : "Ablehnen bestätigen"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
