import api from './api';
import type { SlotDiver, SlotDiverRequest, SlotRegistrationRequest, WaitlistEntry } from '../types';

export const slotDiverService = {
  getBySlot: (slotId: number): Promise<SlotDiver[]> =>
    api.get(`/slots/${slotId}/divers`).then(r => r.data),

  add: (slotId: number, req: SlotDiverRequest): Promise<SlotDiver> =>
    api.post(`/slots/${slotId}/divers`, req).then(r => r.data),

  update: (slotId: number, diverId: number, req: SlotDiverRequest): Promise<SlotDiver> =>
    api.put(`/slots/${slotId}/divers/${diverId}`, req).then(r => r.data),

  remove: (slotId: number, diverId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/divers/${diverId}`).then(r => r.data),

  /** Auto-inscription sur un créneau — retourne l'entrée créée (statut PENDING) */
  registerSelf: (slotId: number, req: SlotRegistrationRequest): Promise<SlotDiver> =>
    api.post(`/slots/${slotId}/divers/register`, req).then(r => r.data),

  /** File d'attente — visible uniquement par le DP affecté */
  getWaitlist: (slotId: number): Promise<WaitlistEntry[]> =>
    api.get(`/slots/${slotId}/divers/waitlist`).then(r => r.data),

  /** Valider une inscription en attente (PENDING → CONFIRMED) */
  validateWaitlistEntry: (slotId: number, diverId: number): Promise<SlotDiver> =>
    api.post(`/slots/${slotId}/divers/waitlist/${diverId}/validate`).then(r => r.data),

  /** Annuler sa propre inscription */
  cancelMyRegistration: (slotId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/divers/registrations/me`).then(r => r.data),
};

