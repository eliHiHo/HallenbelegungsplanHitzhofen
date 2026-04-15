import { api } from "./client";
import type {
  HallStatistics,
  SeriesStatistics,
  SeriesStatisticsDetail,
} from "../types/api";

function buildQs(from?: string, to?: string): string {
  const params = new URLSearchParams();
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export const statisticsApi = {
  getHalls: (from?: string, to?: string) =>
    api.get<HallStatistics[]>(`/statistics/halls${buildQs(from, to)}`),

  getSeries: (from?: string, to?: string) =>
    api.get<SeriesStatistics[]>(`/statistics/series${buildQs(from, to)}`),

  getSeriesDetail: (id: string, from?: string, to?: string) =>
    api.get<SeriesStatisticsDetail>(
      `/statistics/series/${id}${buildQs(from, to)}`
    ),
};
