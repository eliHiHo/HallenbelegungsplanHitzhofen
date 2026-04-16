import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { statisticsApi } from "../shared/api/statistics";

function fmt(n: number, decimals = 0): string {
  return n.toLocaleString("de-DE", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

function fmtDt(iso: string): string {
  return new Date(iso).toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function MyStatisticsPage() {
  const [selectedSeriesId, setSelectedSeriesId] = useState<string | null>(null);

  const {
    data: series,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["my-statistics", "series"],
    queryFn: () => statisticsApi.getSeries(),
  });

  const {
    data: detail,
    isLoading: detailLoading,
    error: detailError,
  } = useQuery({
    queryKey: ["my-statistics", "series", selectedSeriesId],
    queryFn: () => statisticsApi.getSeriesDetail(selectedSeriesId!),
    enabled: selectedSeriesId !== null,
  });

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Meine Statistiken</h1>
      </div>

      <section>
        <h2>Meine Serien</h2>
        {isLoading && <p>Wird geladen…</p>}
        {error && <p className="error">Fehler beim Laden der Statistiken.</p>}
        {series && series.length === 0 && (
          <p className="empty-state">Noch keine Serien vorhanden.</p>
        )}
        {series && series.length > 0 && (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Titel</th>
                  <th>Halle</th>
                  <th>Termine</th>
                  <th>Durchgeführt</th>
                  <th>Abgesagt</th>
                  <th>Teilnehmer</th>
                  <th>Ø Teilnehmer</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {series.map((s) => (
                  <tr
                    key={s.bookingSeriesId}
                    style={
                      selectedSeriesId === s.bookingSeriesId
                        ? { background: "var(--color-bg-selected, #e8f0fe)" }
                        : undefined
                    }
                  >
                    <td>{s.title}</td>
                    <td>{s.hallName}</td>
                    <td>{fmt(s.totalAppointments)}</td>
                    <td>{fmt(s.conductedAppointments)}</td>
                    <td>{fmt(s.cancelledAppointments)}</td>
                    <td>{fmt(s.totalParticipants)}</td>
                    <td>{fmt(s.averageParticipants, 1)}</td>
                    <td>
                      {selectedSeriesId === s.bookingSeriesId ? (
                        <button
                          className="btn-link"
                          onClick={() => setSelectedSeriesId(null)}
                        >
                          Schließen
                        </button>
                      ) : (
                        <button
                          className="btn-link"
                          onClick={() => setSelectedSeriesId(s.bookingSeriesId)}
                        >
                          Details
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {selectedSeriesId && (
        <section style={{ marginTop: "2rem" }}>
          <h2>Seriendetail</h2>
          {detailLoading && <p>Wird geladen…</p>}
          {detailError && (
            <p className="error">Fehler beim Laden der Seriendetails.</p>
          )}
          {detail && (
            <>
              <dl className="detail-list">
                <dt>Titel</dt>
                <dd>{detail.title}</dd>
                <dt>Halle</dt>
                <dd>{detail.hallName}</dd>
                <dt>Termine gesamt</dt>
                <dd>{fmt(detail.totalAppointments)}</dd>
                <dt>Durchgeführt</dt>
                <dd>{fmt(detail.conductedAppointments)}</dd>
                <dt>Abgesagt</dt>
                <dd>{fmt(detail.cancelledAppointments)}</dd>
                <dt>Teilnehmer gesamt</dt>
                <dd>{fmt(detail.totalParticipants)}</dd>
                <dt>Ø Teilnehmer</dt>
                <dd>{fmt(detail.averageParticipants, 1)}</dd>
              </dl>

              {detail.occurrences.length > 0 && (
                <>
                  <h3 style={{ marginTop: "1rem" }}>Einzeltermine</h3>
                  <div className="table-wrapper">
                    <table className="requests-table">
                      <thead>
                        <tr>
                          <th>Von</th>
                          <th>Bis</th>
                          <th>Status</th>
                          <th>Teilnehmer</th>
                          <th>Feedback</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detail.occurrences.map((o) => {
                          const status = o.cancelled
                            ? "Abgesagt"
                            : o.conducted
                            ? "Durchgeführt"
                            : "Geplant";
                          return (
                            <tr key={o.bookingId}>
                              <td>{fmtDt(o.startDateTime)}</td>
                              <td>{fmtDt(o.endDateTime)}</td>
                              <td>{status}</td>
                              <td>
                                {o.participantCount != null
                                  ? fmt(o.participantCount)
                                  : "–"}
                              </td>
                              <td>{o.feedbackComment ?? "–"}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </>
          )}
        </section>
      )}
    </div>
  );
}
