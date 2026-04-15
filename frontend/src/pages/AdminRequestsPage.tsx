import { useState } from "react";
import { useOpenBookingRequests } from "../features/bookingRequests/useBookingRequests";
import BookingRequestDetail from "../features/bookingRequests/BookingRequestDetail";
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

export default function AdminRequestsPage() {
  const { data: requests, isLoading, error } = useOpenBookingRequests();
  const [selected, setSelected] = useState<BookingRequest | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  function handleActionSuccess(message: string) {
    setSuccessMessage(message);
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Offene Buchungsanfragen</h1>
      </div>

      {successMessage && (
        <p className="success-message">{successMessage}</p>
      )}

      {isLoading && <p>Wird geladen…</p>}

      {error && (
        <p className="error">Fehler beim Laden der Anfragen.</p>
      )}

      {!isLoading && !error && requests && requests.length === 0 && (
        <p className="empty-state">Keine offenen Anfragen.</p>
      )}

      {requests && requests.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Titel</th>
                <th>Halle</th>
                <th>Anfragender</th>
                <th>Von</th>
                <th>Bis</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {requests.map((req) => (
                <tr key={req.id}>
                  <td>{req.title}</td>
                  <td>{req.hallName}</td>
                  <td>{req.requestedByName}</td>
                  <td>{formatDateTime(req.startDateTime)}</td>
                  <td>{formatDateTime(req.endDateTime)}</td>
                  <td>
                    <span className="status-badge status-badge--pending">
                      Ausstehend
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn-link"
                      onClick={() => setSelected(req)}
                    >
                      Bearbeiten
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && (
        <BookingRequestDetail
          request={selected}
          onClose={() => setSelected(null)}
          onActionSuccess={handleActionSuccess}
        />
      )}
    </div>
  );
}
