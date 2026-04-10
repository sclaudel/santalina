import api from './api';
import type { SlotDiver, SlotDiverRequest } from '../types';

export const slotDiverService = {
  getBySlot: (slotId: number): Promise<SlotDiver[]> =>
    api.get(`/slots/${slotId}/divers`).then(r => r.data),

  add: (slotId: number, req: SlotDiverRequest): Promise<SlotDiver> =>
    api.post(`/slots/${slotId}/divers`, req).then(r => r.data),

  update: (slotId: number, diverId: number, req: SlotDiverRequest): Promise<SlotDiver> =>
    api.put(`/slots/${slotId}/divers/${diverId}`, req).then(r => r.data),

  remove: (slotId: number, diverId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/divers/${diverId}`).then(r => r.data),

  /** Auto-désinscription du plongeur connecté. */
  cancelMe: (slotId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/divers/me`).then(r => r.data),

  /** Remet un plongeur confirmé dans la liste d'attente (DP / ADMIN). */
  moveToWaitingList: (slotId: number, diverId: number): Promise<void> =>
    api.post(`/slots/${slotId}/divers/${diverId}/move-to-waiting-list`).then(r => r.data),
};
