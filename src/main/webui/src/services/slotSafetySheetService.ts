import api from './api';
import type { SafetySheetFile } from '../types';
import axios from 'axios';

export const slotSafetySheetService = {

  /** Récupère la liste des fiches de sécurité d'un créneau. */
  async list(slotId: number): Promise<SafetySheetFile[]> {
    const res = await api.get<SafetySheetFile[]>(`/slots/${slotId}/safety-sheets`);
    return res.data;
  },

  /**
   * Upload jusqu'à 4 fichiers sur un créneau passé.
   * @returns la liste mise à jour des fiches
   */
  async upload(slotId: number, files: File[]): Promise<SafetySheetFile[]> {
    const form = new FormData();
    files.forEach((f, i) => form.append(`file${i + 1}`, f));
    const res = await api.post<SafetySheetFile[]>(`/slots/${slotId}/safety-sheets`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  /** Télécharge toutes les fiches en ZIP. Retourne un Blob. */
  async downloadZip(slotId: number): Promise<Blob> {
    const res = await api.get<Blob>(`/slots/${slotId}/safety-sheets/zip`, {
      responseType: 'blob',
    });
    return res.data;
  },

  /** Supprime une fiche (admin uniquement). */
  async delete(slotId: number, fileId: number): Promise<void> {
    await api.delete(`/slots/${slotId}/safety-sheets/${fileId}`);
  },

  /** Vérifie si l'utilisateur a accès aux fiches (ne lance pas d'erreur si 403). */
  async canAccess(slotId: number): Promise<boolean> {
    try {
      await api.get(`/slots/${slotId}/safety-sheets`);
      return true;
    } catch (e) {
      if (axios.isAxiosError(e) && (e.response?.status === 403 || e.response?.status === 401)) {
        return false;
      }
      return false;
    }
  },
};
