import api from './api';
import type { DiveSlot, SlotRequest } from '../types';

export const slotService = {
  async getByDate(date: string): Promise<DiveSlot[]> {
    const res = await api.get<DiveSlot[]>('/slots', { params: { date } });
    return res.data;
  },

  async getByWeek(from: string): Promise<DiveSlot[]> {
    const res = await api.get<DiveSlot[]>('/slots/week', { params: { from } });
    return res.data;
  },

  async getById(id: number): Promise<DiveSlot> {
    const res = await api.get<DiveSlot>(`/slots/${id}`);
    return res.data;
  },

  async create(slot: SlotRequest): Promise<DiveSlot> {
    const res = await api.post<DiveSlot>('/slots', slot);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/slots/${id}`);
  },
};

