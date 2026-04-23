import { useState } from "react";
import { useBookingRequests } from "../features/bookingRequests/useBookingRequests";
import { useBookingSeriesRequests } from "../features/bookingSeriesRequests/useBookingSeriesRequests";
import NewRequestModal from "../features/bookingRequests/NewRequestModal";
import { DAY_OF_WEEK_LABELS, formatTime } from "../shared/lib/dayOfWeek";
import type { BookingRequest, BookingSeriesRequest } from "../shared/types/api";

// ── Formatierung ───────────────────────────────────────────────────────────────

function fmtDateTime(iso: string): string {
  return new Date(iso).toLocaleString("de-DE", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString("de-DE", {
    day: "2-digit", month: "2-digit", year: "numeric",
  });
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Ausstehend",
  APPROVED: "Genehmigt",
  REJECTED: "Abgelehnt",
};

// ── Zeilen-Komponenten ─────────────────────────────────────────────────────────

function SingleRow({ req }: { req: BookingRequest }) {
  return (
    <tr>
      <td>{req.title}</td>
      <td>{req.hallName}</td>
      <td>{fmtDateTime(req.startDateTime)}</td>
      <td>{fmtDateTime(req.endDateTime)}</td>
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

function SeriesRow({ req }: { req: BookingSeriesRequest }) {
  return (
    <tr>
      <td>{req.title}</td>
      <td>{req.hallName}</td>
      <td>{DAY_OF_WEEK_LABELS[req.weekday]}</td>
      <td>{formatTime(req.startTime)} – {formatTime(req.endTime)}</td>
      <td>{fmtDate(req.startDate)} – {fmtDate(req.endDate)}</td>
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

// ── Hauptseite ─────────────────────────────────────────────────────────────────

type Tab = "single" | "series";

export default function MyRequestsPage() {
  const [tab, setTab] = useState<Tab>("single");
  const [showModal, setShowModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { data: singleRequests, isLoading: singleLoading, error: singleError } =
    useBookingRequests();
  const { data: seriesRequests, isLoading: seriesLoading, error: seriesError } =
    useBookingSeriesRequests();

  function handleSuccess(message: string) {
    setShowModal(false);
    setSuccessMessage(message);
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  const isLoading = tab === "single" ? singleLoading : seriesLoading;
  const hasError  = tab === "single" ? !!singleError : !!seriesError;

  return (
    <div className="my-requests-page">
      <div className="page-header">
        <h1>Anfragen</h1>
        <button
          className="btn-primary"
          onClick={() => { setSuccessMessage(null); setShowModal(true); }}
        >
          + Neue Anfrage
        </button>
      </div>

      {successMessage && <p className="success-message">{successMessage}</p>}

      {/* Tab-Umschalter */}
      <div className="req-tab-bar">
        <button
          className={`req-tab-btn${tab === "single" ? " req-tab-btn--active" : ""}`}
          onClick={() => setTab("single")}
        >
          Einzelanfragen
        </button>
        <button
          className={`req-tab-btn${tab === "series" ? " req-tab-btn--active" : ""}`}
          onClick={() => setTab("series")}
        >
          Serienanfragen
        </button>
      </div>

      {isLoading && <p>Wird geladen…</p>}
      {hasError && <p className="error">Fehler beim Laden der Anfragen.</p>}

      {/* Einzelanfragen-Tabelle */}
      {tab === "single" && !singleLoading && !singleError && (
        singleRequests && singleRequests.length > 0 ? (
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
                {singleRequests.map((req) => (
                  <SingleRow key={req.id} req={req} />
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="empty-state">Noch keine Einzelanfragen vorhanden.</p>
        )
      )}

      {/* Serienanfragen-Tabelle */}
      {tab === "series" && !seriesLoading && !seriesError && (
        seriesRequests && seriesRequests.length > 0 ? (
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
                {seriesRequests.map((req) => (
                  <SeriesRow key={req.id} req={req} />
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="empty-state">Noch keine Serienanfragen vorhanden.</p>
        )
      )}

      {showModal && (
        <NewRequestModal
          onClose={() => setShowModal(false)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
}
