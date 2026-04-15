import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import CancelBookingModal from "./CancelBookingModal";
import FeedbackModal from "./FeedbackModal";
import EditBookingModal from "./EditBookingModal";
import type { CalendarEntry } from "../../shared/types/api";

interface Props {
  entry: CalendarEntry;
  onClose: () => void;
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
  APPROVED: "Genehmigt",
  CANCELLED: "Storniert",
  COMPLETED: "Abgeschlossen",
};

export default function CalendarEntryDetail({ entry, onClose }: Props) {
  const { user } = useAuth();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);

  const isOwnBooking =
    user?.role === "CLUB_REPRESENTATIVE" &&
    entry.type === "BOOKING" &&
    entry.ownEntry;

  // Cancel: only for non-cancelled own bookings
  const showCancelButton = isOwnBooking && entry.status !== "CANCELLED";

  // Feedback: backend only blocks cancelled bookings; canViewFeedback is
  // verified inside FeedbackModal via GET /bookings/{id}.
  const showFeedbackButton = isOwnBooking && entry.status !== "CANCELLED";

  // Admin edit: canEdit is enforced by backend; backend verifies role.
  const showEditButton =
    user?.role === "ADMIN" &&
    entry.type === "BOOKING" &&
    entry.status !== "CANCELLED";

  function handleCancelSuccess() {
    setShowCancelModal(false);
    onClose();
  }

  function handleFeedbackSuccess() {
    setShowFeedbackModal(false);
    onClose();
  }

  return (
    <>
      <div className="entry-detail-overlay" onClick={onClose}>
        <div
          className="entry-detail-panel"
          onClick={(e) => e.stopPropagation()}
        >
          <button className="entry-detail-close" onClick={onClose}>✕</button>
          <h2>{entry.title}</h2>

          <dl className="detail-list">
            <dt>Halle</dt>
            <dd>{entry.hallName}</dd>

            <dt>Von</dt>
            <dd>{formatDateTime(entry.startDateTime)}</dd>

            <dt>Bis</dt>
            <dd>{formatDateTime(entry.endDateTime)}</dd>

            {entry.responsibleUserName && (
              <>
                <dt>Verantwortlich</dt>
                <dd>{entry.responsibleUserName}</dd>
              </>
            )}

            {entry.description && (
              <>
                <dt>Beschreibung</dt>
                <dd>{entry.description}</dd>
              </>
            )}

            {entry.status && (
              <>
                <dt>Status</dt>
                <dd>{STATUS_LABELS[entry.status] ?? entry.status}</dd>
              </>
            )}
          </dl>

          {(showCancelButton || showFeedbackButton || showEditButton) && (
            <div className="detail-actions">
              {showFeedbackButton && (
                <button
                  className="btn-primary"
                  onClick={() => setShowFeedbackModal(true)}
                >
                  Feedback
                </button>
              )}
              {showEditButton && (
                <button
                  className="btn-primary"
                  onClick={() => setShowEditModal(true)}
                >
                  Bearbeiten
                </button>
              )}
              {showCancelButton && (
                <button
                  className="btn-reject"
                  onClick={() => setShowCancelModal(true)}
                >
                  Stornieren
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {showCancelModal && (
        <CancelBookingModal
          bookingId={entry.id}
          bookingTitle={entry.title}
          onClose={() => setShowCancelModal(false)}
          onSuccess={handleCancelSuccess}
        />
      )}

      {showFeedbackModal && (
        <FeedbackModal
          bookingId={entry.id}
          bookingTitle={entry.title}
          onClose={() => setShowFeedbackModal(false)}
          onSuccess={handleFeedbackSuccess}
        />
      )}

      {showEditModal && (
        <EditBookingModal
          bookingId={entry.id}
          bookingTitle={entry.title}
          onClose={() => setShowEditModal(false)}
          onSuccess={() => {
            setShowEditModal(false);
            onClose();
          }}
        />
      )}
    </>
  );
}
