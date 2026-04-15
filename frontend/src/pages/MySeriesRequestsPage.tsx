import { useState } from "react";
import { useBookingSeriesRequests } from "../features/bookingSeriesRequests/useBookingSeriesRequests";
import BookingSeriesRequestForm from "../features/bookingSeriesRequests/BookingSeriesRequestForm";
import { DAY_OF_WEEK_LABELS, formatTime } from "../shared/lib/dayOfWeek";
import type { BookingSeriesRequest } from "../shared/types/api";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Ausstehend",
  APPROVED: "Genehmigt",
  REJECTED: "Abgelehnt",
};

function SeriesRow({ req }: { req: BookingSeriesRequest }) {
  return (
    <tr>
      <td>{req.title}</td>
      <td>{req.hallName}</td>
      <td>{DAY_OF_WEEK_LABELS[req.weekday]}</td>
      <td>{formatTime(req.startTime)} – {formatTime(req.endTime)}</td>
      <td>{formatDate(req.startDate)} – {formatDate(req.endDate)}</td>
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

export default function MySeriesRequestsPage() {
  const [showForm, setShowForm] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { data: requests, isLoading, error } = useBookingSeriesRequests();

  function handleSuccess() {
    setShowForm(false);
    setSuccessMessage("Serienanfrage wurde erfolgreich gesendet.");
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  return (
    <div className="my-requests-page">
      <div className="page-header">
        <h1>Meine Serienanfragen</h1>
        <button
          className="btn-primary"
          onClick={() => {
            setSuccessMessage(null);
            setShowForm(true);
          }}
        >
          + Neue Serienanfrage
        </button>
      </div>

      {successMessage && <p className="success-message">{successMessage}</p>}
      {isLoading && <p>Wird geladen…</p>}
      {error && <p className="error">Fehler beim Laden der Anfragen.</p>}

      {!isLoading && !error && requests?.length === 0 && (
        <p className="empty-state">Noch keine Serienanfragen vorhanden.</p>
      )}

      {requests && requests.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Titel</th>
                <th>Halle</th>
                <th>Wochentag</th>
                <th>Uhrzeit</th>
                <th>Zeitraum</th>
                <th>Status</th>
                <th>Ablehnungsgrund</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((req) => (
                <SeriesRow key={req.id} req={req} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showForm && (
        <BookingSeriesRequestForm
          onClose={() => setShowForm(false)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
}
