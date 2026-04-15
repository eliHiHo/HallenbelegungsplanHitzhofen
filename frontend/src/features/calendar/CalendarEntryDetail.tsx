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

export default function CalendarEntryDetail({ entry, onClose }: Props) {
  return (
    <div className="entry-detail-overlay" onClick={onClose}>
      <div
        className="entry-detail-panel"
        onClick={(e) => e.stopPropagation()}
      >
        <button className="entry-detail-close" onClick={onClose}>
          ✕
        </button>
        <h2>{entry.title}</h2>
        <dl>
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
              <dd>{entry.status}</dd>
            </>
          )}
        </dl>
      </div>
    </div>
  );
}
