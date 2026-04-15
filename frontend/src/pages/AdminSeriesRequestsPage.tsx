import { useState } from "react";
import { useOpenBookingSeriesRequests } from "../features/bookingSeriesRequests/useBookingSeriesRequests";
import BookingSeriesRequestDetail from "../features/bookingSeriesRequests/BookingSeriesRequestDetail";
import { DAY_OF_WEEK_LABELS, formatTime } from "../shared/lib/dayOfWeek";
import type { BookingSeriesRequest } from "../shared/types/api";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export default function AdminSeriesRequestsPage() {
  const { data: requests, isLoading, error } = useOpenBookingSeriesRequests();
  const [selected, setSelected] = useState<BookingSeriesRequest | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  function handleActionSuccess(message: string) {
    setSuccessMessage(message);
    setTimeout(() => setSuccessMessage(null), 5000);
  }

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Offene Serienanfragen</h1>
      </div>

      {successMessage && <p className="success-message">{successMessage}</p>}
      {isLoading && <p>Wird geladen…</p>}
      {error && <p className="error">Fehler beim Laden der Anfragen.</p>}

      {!isLoading && !error && requests?.length === 0 && (
        <p className="empty-state">Keine offenen Serienanfragen.</p>
      )}

      {requests && requests.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Titel</th>
                <th>Halle</th>
                <th>Anfragender</th>
                <th>Wochentag</th>
                <th>Uhrzeit</th>
                <th>Zeitraum</th>
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
                  <td>{DAY_OF_WEEK_LABELS[req.weekday]}</td>
                  <td>{formatTime(req.startTime)} – {formatTime(req.endTime)}</td>
                  <td>{formatDate(req.startDate)} – {formatDate(req.endDate)}</td>
                  <td>
                    <span className="status-badge status-badge--pending">
                      Ausstehend
                    </span>
                  </td>
                  <td>
                    <button className="btn-link" onClick={() => setSelected(req)}>
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
        <BookingSeriesRequestDetail
          request={selected}
          onClose={() => setSelected(null)}
          onActionSuccess={handleActionSuccess}
        />
      )}
    </div>
  );
}
