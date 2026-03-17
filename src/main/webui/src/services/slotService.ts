import api from './api';
import type { DiveSlot, SlotRequest, BatchSlotResponse } from '../types';

export interface SlotInfoUpdate {
  title?: string;
  notes?: string;
  slotType?: string;
  club?: string;
  slotDate?: string;
  startTime?: string;
  endTime?: string;
}

export const slotService = {
  async getByDate(date: string): Promise<DiveSlot[]> {
    const res = await api.get<DiveSlot[]>('/slots', { params: { date } });
    return res.data;
  },

  async getByWeek(from: string): Promise<DiveSlot[]> {
    const res = await api.get<DiveSlot[]>('/slots/week', { params: { from } });
    return res.data;
  },

  async getByMonth(year: number, month: number): Promise<DiveSlot[]> {
    const res = await api.get<DiveSlot[]>('/slots/month', { params: { year, month } });
    return res.data;
  },

  async getById(id: number): Promise<DiveSlot> {
    const res = await api.get<DiveSlot>(`/slots/${id}`);
    return res.data;
  },

  async create(slot: SlotRequest): Promise<BatchSlotResponse> {
    const res = await api.post<BatchSlotResponse>('/slots', slot);
    return res.data;
  },

  async updateDiverCount(id: number, diverCount: number): Promise<DiveSlot> {
    const res = await api.patch<DiveSlot>(`/slots/${id}/diver-count`, { diverCount });
    return res.data;
  },

  async updateSlotInfo(id: number, info: SlotInfoUpdate): Promise<DiveSlot> {
    const res = await api.patch<DiveSlot>(`/slots/${id}/info`, info);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/slots/${id}`);
  },
};
