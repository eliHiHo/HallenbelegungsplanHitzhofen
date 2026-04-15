import { useState } from "react";
import { useBookingRequests } from "../features/bookingRequests/useBookingRequests";
import BookingRequestForm from "../features/bookingRequests/BookingRequestForm";
import type { BookingRequest } from "../shared/types/api";

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("de-DE", {
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

function RequestRow({ req }: { req: BookingRequest }) {
  return (
    <tr>
      <td>{req.title}</td>
      <td>{req.hallName}</td>
      <td>{formatDateTime(req.startDateTime)}</td>
      <td>{formatDateTime(req.endDateTime)}</td>
      <td>
        <span className={`status-badge status-badge--${req.status.toLowerCase()}`}>
          {STATUS_LABELS[req.status] ?? req.status}
        </span>
      </td>
      <td>
        {req.rejectionReason && (
          <span className="rejection-reason">{req.rejectionReason}</span>
        )}
      </td>
    </tr>
  );
}

export default function MyRequestsPage() {
  const [showForm, setShowForm] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { data: requests, isLoading, error } = useBookingRequests();

  function handleSuccess() {
    setShowForm(false);
    setSuccessMessage("Anfrage wurde erfolgreich gesendet.");
    // Clear success message after 4 seconds
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  return (
    <div className="my-requests-page">
      <div className="page-header">
        <h1>Meine Anfragen</h1>
        <button
          className="btn-primary"
          onClick={() => {
            setSuccessMessage(null);
            setShowForm(true);
          }}
        >
          + Neue Anfrage
        </button>
      </div>

      {successMessage && (
        <p className="success-message">{successMessage}</p>
      )}

      {isLoading && <p>Wird geladen…</p>}

      {error && (
        <p className="error">Fehler beim Laden der Anfragen.</p>
      )}

      {!isLoading && !error && requests && requests.length === 0 && (
        <p className="empty-state">Noch keine Anfragen vorhanden.</p>
      )}

      {requests && requests.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Titel</th>
                <th>Halle</th>
                <th>Von</th>
                <th>Bis</th>
                <th>Status</th>
                <th>Ablehnungsgrund</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((req) => (
                <RequestRow key={req.id} req={req} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showForm && (
        <BookingRequestForm
          onClose={() => setShowForm(false)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
}
