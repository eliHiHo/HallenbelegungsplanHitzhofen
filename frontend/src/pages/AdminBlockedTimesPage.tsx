import { useState } from "react";
import {
  useBlockedTimes,
  useDeleteBlockedTime,
} from "../features/blockedTimes/useBlockedTimes";
import BlockedTimeForm from "../features/blockedTimes/BlockedTimeForm";
import type { BlockedTime } from "../shared/types/api";

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function BlockedTimeRow({
  bt,
  onDelete,
}: {
  bt: BlockedTime;
  onDelete: (id: string) => void;
}) {
  const [confirming, setConfirming] = useState(false);
  const { mutateAsync: deleteBt, isPending } = useDeleteBlockedTime();

  async function handleDelete() {
    await deleteBt(bt.id);
    onDelete(bt.id);
  }

  return (
    <tr>
      <td>{bt.hallName}</td>
      <td>{formatDateTime(bt.startDateTime)}</td>
      <td>{formatDateTime(bt.endDateTime)}</td>
      <td>{bt.reason ?? "–"}</td>
      <td>
        {!confirming ? (
          <button className="btn-link" onClick={() => setConfirming(true)}>
            Löschen
          </button>
        ) : (
          <span className="delete-confirm">
            Sicher?{" "}
            <button
              className="btn-reject-inline"
              onClick={handleDelete}
              disabled={isPending}
            >
              {isPending ? "…" : "Ja"}
            </button>{" "}
            <button
              className="btn-link"
              onClick={() => setConfirming(false)}
              disabled={isPending}
            >
              Nein
            </button>
          </span>
        )}
      </td>
    </tr>
  );
}

export default function AdminBlockedTimesPage() {
  const { data: blockedTimes, isLoading, error } = useBlockedTimes();
  const [showForm, setShowForm] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  function handleSuccess() {
    setShowForm(false);
    setSuccessMessage("Sperrzeit wurde angelegt.");
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  function handleDeleteSuccess() {
    setSuccessMessage("Sperrzeit wurde gelöscht.");
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Sperrzeiten</h1>
        <button
          className="btn-primary"
          onClick={() => {
            setSuccessMessage(null);
            setShowForm(true);
          }}
        >
          + Neue Sperrzeit
        </button>
      </div>

      {successMessage && <p className="success-message">{successMessage}</p>}
      {isLoading && <p>Wird geladen…</p>}
      {error && <p className="error">Fehler beim Laden der Sperrzeiten.</p>}

      {!isLoading && !error && blockedTimes?.length === 0 && (
        <p className="empty-state">Keine Sperrzeiten vorhanden.</p>
      )}

      {blockedTimes && blockedTimes.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Halle</th>
                <th>Von</th>
                <th>Bis</th>
                <th>Grund</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {blockedTimes.map((bt) => (
                <BlockedTimeRow
                  key={bt.id}
                  bt={bt}
                  onDelete={handleDeleteSuccess}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showForm && (
        <BlockedTimeForm
          onClose={() => setShowForm(false)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
}
