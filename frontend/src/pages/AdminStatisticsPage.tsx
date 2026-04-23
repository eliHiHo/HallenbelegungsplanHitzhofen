import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { statisticsApi } from "../shared/api/statistics";
import {
  useCancelBookingSeries,
  useCancelSeriesOccurrence,
} from "../features/bookingSeries/useCancelBookingSeries";
import { ApiError } from "../shared/api/client";

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

export default function AdminStatisticsPage() {
  const [selectedSeriesId, setSelectedSeriesId] = useState<string | null>(null);
  const [cancelSeriesConfirm, setCancelSeriesConfirm] = useState(false);
  const [cancelOccurrenceId, setCancelOccurrenceId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const { mutateAsync: cancelSeries, isPending: cancelingSeriesPending } =
    useCancelBookingSeries();
  const { mutateAsync: cancelOccurrence, isPending: cancelingOccurrencePending } =
    useCancelSeriesOccurrence();

  async function handleCancelSeries() {
    if (!selectedSeriesId) return;
    setActionError(null);
    try {
      await cancelSeries({ seriesId: selectedSeriesId });
      setCancelSeriesConfirm(false);
      setSelectedSeriesId(null);
      setActionSuccess("Serie wurde storniert.");
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Fehler beim Stornieren der Serie.");
    }
  }

  async function handleCancelOccurrence(bookingId: string) {
    if (!selectedSeriesId) return;
    setActionError(null);
    try {
      await cancelOccurrence({ seriesId: selectedSeriesId, bookingId });
      setCancelOccurrenceId(null);
      setActionSuccess("Termin wurde storniert.");
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Fehler beim Stornieren des Termins.");
    }
  }

  const {
    data: halls,
    isLoading: hallsLoading,
    error: hallsError,
  } = useQuery({
    queryKey: ["statistics", "halls"],
    queryFn: () => statisticsApi.getHalls(),
  });

  const {
    data: series,
    isLoading: seriesLoading,
    error: seriesError,
  } = useQuery({
    queryKey: ["statistics", "series"],
    queryFn: () => statisticsApi.getSeries(),
  });

  const {
    data: detail,
    isLoading: detailLoading,
    error: detailError,
  } = useQuery({
    queryKey: ["statistics", "series", selectedSeriesId],
    queryFn: () => statisticsApi.getSeriesDetail(selectedSeriesId!),
    enabled: selectedSeriesId !== null,
  });

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Statistiken</h1>
      </div>

      {actionSuccess && <p className="success-message">{actionSuccess}</p>}
      {actionError && <p className="error">{actionError}</p>}

      {/* Hall Statistics */}
      <section>
        <h2>Hallenauslastung</h2>
        {hallsLoading && <p>Wird geladen…</p>}
        {hallsError && <p className="error">Fehler beim Laden der Hallenstatistiken.</p>}
        {halls && halls.length === 0 && (
          <p className="empty-state">Keine Daten vorhanden.</p>
        )}
        {halls && halls.length > 0 && (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Halle</th>
                  <th>Buchungen gesamt</th>
                  <th>Davon abgesagt</th>
                  <th>Teilnehmer gesamt</th>
                  <th>Auslastung</th>
                  <th>Top-Serien</th>
                </tr>
              </thead>
              <tbody>
                {halls.map((h) => (
                  <tr key={h.hallId}>
                    <td>{h.hallName}</td>
                    <td>{fmt(h.totalBookings)}</td>
                    <td>{fmt(h.cancelledBookings)}</td>
                    <td>{fmt(h.totalParticipants)}</td>
                    <td>{fmt(h.utilizationPercent, 1)} %</td>
                    <td>
                      {h.topSeries.length === 0
                        ? "–"
                        : h.topSeries
                            .map((s) => `${s.title} (${s.bookingCount})`)
                            .join(", ")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Series Overview */}
      <section style={{ marginTop: "2rem" }}>
        <h2>Serien</h2>
        {seriesLoading && <p>Wird geladen…</p>}
        {seriesError && <p className="error">Fehler beim Laden der Serienstatistiken.</p>}
        {series && series.length === 0 && (
          <p className="empty-state">Keine Serien vorhanden.</p>
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

      {/* Series Detail */}
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
                <dt>Verantwortliche Person</dt>
                <dd>{detail.responsibleUserName}</dd>
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

              {/* Cancel whole series */}
              <div style={{ marginTop: "1rem" }}>
                {!cancelSeriesConfirm ? (
                  <button
                    className="btn-reject"
                    onClick={() => setCancelSeriesConfirm(true)}
                  >
                    Gesamte Serie stornieren
                  </button>
                ) : (
                  <span className="delete-confirm">
                    Alle zukünftigen Termine dieser Serie stornieren?{" "}
                    <button
                      className="btn-reject-inline"
                      onClick={handleCancelSeries}
                      disabled={cancelingSeriesPending}
                    >
                      {cancelingSeriesPending ? "…" : "Ja, stornieren"}
                    </button>{" "}
                    <button
                      className="btn-link"
                      onClick={() => setCancelSeriesConfirm(false)}
                      disabled={cancelingSeriesPending}
                    >
                      Abbrechen
                    </button>
                  </span>
                )}
              </div>

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
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {detail.occurrences.map((o) => {
                          const status = o.cancelled
                            ? "Abgesagt"
                            : o.conducted
                            ? "Durchgeführt"
                            : "Geplant";
                          const isConfirming = cancelOccurrenceId === o.bookingId;
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
                              <td>
                                {!o.cancelled && !isConfirming && (
                                  <button
                                    className="btn-link"
                                    onClick={() =>
                                      setCancelOccurrenceId(o.bookingId)
                                    }
                                  >
                                    Stornieren
                                  </button>
                                )}
                                {!o.cancelled && isConfirming && (
                                  <span className="delete-confirm">
                                    Sicher?{" "}
                                    <button
                                      className="btn-reject-inline"
                                      onClick={() =>
                                        handleCancelOccurrence(o.bookingId)
                                      }
                                      disabled={cancelingOccurrencePending}
                                    >
                                      {cancelingOccurrencePending ? "…" : "Ja"}
                                    </button>{" "}
                                    <button
                                      className="btn-link"
                                      onClick={() =>
                                        setCancelOccurrenceId(null)
                                      }
                                      disabled={cancelingOccurrencePending}
                                    >
                                      Nein
                                    </button>
                                  </span>
                                )}
                              </td>
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
