import api from './api';
import type { WaitingListEntry, WaitingListRequest, UpdateRegistrationRequest, RegistrationStatus } from '../types';
import type { DiveSlot } from '../types';

export const waitingListService = {
  /** Inscription JSON simple (créneau sans pièces jointes obligatoires). */
  register: (slotId: number, req: WaitingListRequest): Promise<WaitingListEntry> =>
    api.post(`/slots/${slotId}/waiting-list`, req).then(r => r.data),

  /** Inscription multipart avec pièces jointes (certificat médical + QR code). */
  registerWithAttachments: (
    slotId: number,
    req: WaitingListRequest,
    medicalCert: File,
    licenseQr: File,
  ): Promise<WaitingListEntry> => {
    const form = new FormData();
    form.append('data', JSON.stringify(req));
    form.append('medicalCert', medicalCert);
    form.append('licenseQr', licenseQr);
    return api.post(`/slots/${slotId}/waiting-list/with-attachments`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  /** Lecture de la liste d'attente (DP / ADMIN seulement). */
  getWaitingList: (slotId: number): Promise<WaitingListEntry[]> =>
    api.get(`/slots/${slotId}/waiting-list`).then(r => r.data),

  /** Valider une entrée → la transfère dans slot_divers. */
  approve: (slotId: number, entryId: number): Promise<void> =>
    api.post(`/slots/${slotId}/waiting-list/${entryId}/approve`).then(r => r.data),

  /** Mettre à jour le statut de vérification d'un dossier (DP / ADMIN). */
  updateStatus: (
    slotId: number,
    entryId: number,
    status: RegistrationStatus,
    reason?: string,
  ): Promise<WaitingListEntry> =>
    api.patch(`/slots/${slotId}/waiting-list/${entryId}/status`, { status, reason }).then(r => r.data),

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

  /** Construit l'URL de téléchargement d'une pièce jointe (nécessite le token dans le header de l'API). */
  getAttachmentUrl: (slotId: number, entryId: number, type: 'medical-cert' | 'license-qr'): string =>
    `/api/attachments/${slotId}/${entryId}/${type}`,
};
