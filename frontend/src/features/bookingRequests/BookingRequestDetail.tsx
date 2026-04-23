import { useState, type FormEvent } from "react";
import { useApproveBookingRequest, useRejectBookingRequest } from "./useBookingRequests";
import { ApiError } from "../../shared/api/client";
import type { BookingRequest } from "../../shared/types/api";

interface Props {
  request: BookingRequest;
  onClose: () => void;
  onActionSuccess: (message: string) => void;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("de-DE", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Ausstehend",
  APPROVED: "Genehmigt",
  REJECTED: "Abgelehnt",
};

export default function BookingRequestDetail({ request, onClose, onActionSuccess }: Props) {
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const approve = useApproveBookingRequest();
  const reject = useRejectBookingRequest();

  const isPending = request.status === "PENDING";
  const isActing = approve.isPending || reject.isPending;

  async function handleApprove() {
    setError(null);
    try {
      await approve.mutateAsync(request.id);
      onActionSuccess("Anfrage wurde genehmigt.");
      onClose();
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
      onActionSuccess("Anfrage wurde abgelehnt.");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Fehler beim Ablehnen. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>{request.title}</h2>

        <dl className="detail-list">
          <dt>Halle</dt>
          <dd>{request.hallName}</dd>

          <dt>Von</dt>
          <dd>{formatDateTime(request.startDateTime)}</dd>

          <dt>Bis</dt>
          <dd>{formatDateTime(request.endDateTime)}</dd>

          <dt>Anfragender</dt>
          <dd>{request.requestedByName}</dd>

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
              <label htmlFor="reject-reason">Ablehnungsgrund</label>
              <textarea
                id="reject-reason"
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
