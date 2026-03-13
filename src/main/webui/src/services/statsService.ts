import api from './api';
import type { StatsResponse, MyStatsResponse } from '../types';

export const statsService = {
  async getStats(from?: string, to?: string): Promise<StatsResponse> {
    const params: Record<string, string> = {};
    if (from) params.from = from;
    if (to)   params.to   = to;
    const res = await api.get<StatsResponse>('/stats', { params });
    return res.data;
  },

  async getMyStats(from?: string, to?: string): Promise<MyStatsResponse> {
    const params: Record<string, string> = {};
    if (from) params.from = from;
    if (to)   params.to   = to;
    const res = await api.get<MyStatsResponse>('/stats/my', { params });
    return res.data;
  },
};
