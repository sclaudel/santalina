import api from './api';
import type { SlotDive, SlotDiveRequest } from '../types';

export const slotDiveService = {
  getBySlot: (slotId: number): Promise<SlotDive[]> =>
    api.get<SlotDive[]>(`/slots/${slotId}/dives`).then(r => r.data),

  create: (slotId: number, req?: SlotDiveRequest): Promise<SlotDive> =>
    api.post<SlotDive>(`/slots/${slotId}/dives`, req ?? {}).then(r => r.data),

  update: (slotId: number, diveId: number, req: SlotDiveRequest): Promise<SlotDive> =>
    api.patch<SlotDive>(`/slots/${slotId}/dives/${diveId}`, req).then(r => r.data),

  delete: (slotId: number, diveId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/dives/${diveId}`).then(() => undefined),

  /** Assigne ou désassigne une palanquée à une plongée (slotDiveId=null = désassigner) */
  assignPalanquee: (slotId: number, palanqueeId: number, slotDiveId: number | null): Promise<void> =>
    api.put(`/slots/${slotId}/dives/assign`, { palanqueeId, slotDiveId }).then(() => undefined),
};
