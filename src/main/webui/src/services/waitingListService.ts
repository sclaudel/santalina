import api from './api';
import type { WaitingListEntry, WaitingListRequest, UpdateRegistrationRequest } from '../types';
import type { DiveSlot } from '../types';

export const waitingListService = {
  /** Inscription d'un plongeur en liste d'attente. */
  register: (slotId: number, req: WaitingListRequest): Promise<WaitingListEntry> =>
    api.post(`/slots/${slotId}/waiting-list`, req).then(r => r.data),

  /** Lecture de la liste d'attente (DP / ADMIN seulement). */
  getWaitingList: (slotId: number): Promise<WaitingListEntry[]> =>
    api.get(`/slots/${slotId}/waiting-list`).then(r => r.data),

  /** Valider une entrée → la transfère dans slot_divers. */
  approve: (slotId: number, entryId: number): Promise<void> =>
    api.post(`/slots/${slotId}/waiting-list/${entryId}/approve`).then(r => r.data),

  /** Récupère l'entrée en liste d'attente de l'utilisateur connecté pour ce créneau (null si absent). */
  getMyEntry: (slotId: number): Promise<WaitingListEntry | null> =>
    api.get(`/slots/${slotId}/waiting-list/me`)
      .then(r => r.data as WaitingListEntry)
      .catch(err => err?.response?.status === 404 ? null : Promise.reject(err)),

  /** Annuler une entrée (plongeur lui-même ou DP/ADMIN). */
  cancel: (slotId: number, entryId: number): Promise<void> =>
    api.delete(`/slots/${slotId}/waiting-list/${entryId}`).then(r => r.data),

  /** Modifier les paramètres d'inscription libre du créneau (DP / ADMIN). */
  updateRegistration: (slotId: number, req: UpdateRegistrationRequest): Promise<DiveSlot> =>
    api.patch(`/slots/${slotId}/registration`, req).then(r => r.data),
};
