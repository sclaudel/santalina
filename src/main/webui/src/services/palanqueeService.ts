import api from './api';
import type { Palanquee } from '../types';

export const palanqueeService = {
  getBySlot: (slotId: number): Promise<Palanquee[]> =>
    api.get<Palanquee[]>(`/slots/${slotId}/palanquees`).then(r => r.data),

  create: (slotId: number, name: string): Promise<Palanquee> =>
    api.post<Palanquee>(`/slots/${slotId}/palanquees`, { name }).then(r => r.data),

  rename: (slotId: number, id: number, name: string, depth?: string, duration?: string): Promise<Palanquee> =>
    api.put<Palanquee>(`/slots/${slotId}/palanquees/${id}`, { name, depth, duration }).then(r => r.data),

  delete: (slotId: number, id: number): Promise<void> =>
    api.delete(`/slots/${slotId}/palanquees/${id}`).then(() => undefined),

  assign: (slotId: number, diverId: number, palanqueeId: number | null, fromPalanqueeId?: number | null): Promise<void> =>
    api.put(`/slots/${slotId}/palanquees/assign`, { diverId, palanqueeId, fromPalanqueeId }).then(() => undefined),

  reorder: (slotId: number, palanqueeId: number, diverIds: number[]): Promise<void> =>
    api.put(`/slots/${slotId}/palanquees/${palanqueeId}/reorder`, { diverIds }).then(() => undefined),

  /** Met à jour les aptitudes spécifiques à une plongée pour un membre d'une palanquée. */
  updateMemberAptitudes: (slotId: number, palanqueeId: number, diverId: number, aptitudes?: string): Promise<void> =>
    api.patch(`/slots/${slotId}/palanquees/${palanqueeId}/members/${diverId}/aptitudes`, { aptitudes: aptitudes ?? null }).then(() => undefined),
};
