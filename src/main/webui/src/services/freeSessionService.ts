import api from './api';
import type {
  FreeDiveSession,
  FreeSessionDiver,
  FreePalanquee,
  FreeSessionDive,
} from '../types';

const BASE = '/free-sessions';

// ── Sessions ──────────────────────────────────────────────────────────────────

export const freeSessionService = {
  list: (): Promise<FreeDiveSession[]> =>
    api.get<FreeDiveSession[]>(BASE).then(r => r.data),

  create: (label: string | null, diveDate: string, startTime: string, notes?: string | null): Promise<FreeDiveSession> =>
    api.post<FreeDiveSession>(BASE, { label, diveDate, startTime, notes }).then(r => r.data),

  update: (id: number, label: string | null, diveDate: string, startTime: string, notes?: string | null): Promise<FreeDiveSession> =>
    api.put<FreeDiveSession>(`${BASE}/${id}`, { label, diveDate, startTime, notes }).then(r => r.data),

  delete: (id: number): Promise<void> =>
    api.delete(`${BASE}/${id}`).then(() => undefined),

  copy: (sourceId: number, label: string | null, diveDate: string, startTime: string): Promise<FreeDiveSession> =>
    api.post<FreeDiveSession>(`${BASE}/${sourceId}/copy`, { label, diveDate, startTime }).then(r => r.data),

  // ── Plongeurs ───────────────────────────────────────────────────────────────

  listDivers: (id: number): Promise<FreeSessionDiver[]> =>
    api.get<FreeSessionDiver[]>(`${BASE}/${id}/divers`).then(r => r.data),

  addDiver: (id: number, req: Omit<FreeSessionDiver, 'id'>): Promise<FreeSessionDiver> =>
    api.post<FreeSessionDiver>(`${BASE}/${id}/divers`, req).then(r => r.data),

  updateDiver: (id: number, did: number, req: Omit<FreeSessionDiver, 'id'>): Promise<FreeSessionDiver> =>
    api.put<FreeSessionDiver>(`${BASE}/${id}/divers/${did}`, req).then(r => r.data),

  removeDiver: (id: number, did: number): Promise<void> =>
    api.delete(`${BASE}/${id}/divers/${did}`).then(() => undefined),

  // ── Plongées ────────────────────────────────────────────────────────────────

  listDives: (id: number): Promise<FreeSessionDive[]> =>
    api.get<FreeSessionDive[]>(`${BASE}/${id}/dives`).then(r => r.data),

  createDive: (id: number, req?: Partial<Omit<FreeSessionDive, 'id' | 'diveIndex'>>): Promise<FreeSessionDive> =>
    api.post<FreeSessionDive>(`${BASE}/${id}/dives`, req ?? {}).then(r => r.data),

  updateDive: (id: number, diveId: number, req: Partial<Omit<FreeSessionDive, 'id' | 'diveIndex'>>): Promise<FreeSessionDive> =>
    api.patch<FreeSessionDive>(`${BASE}/${id}/dives/${diveId}`, req).then(r => r.data),

  deleteDive: (id: number, diveId: number): Promise<void> =>
    api.delete(`${BASE}/${id}/dives/${diveId}`).then(() => undefined),

  assignPalanqueeToDive: (id: number, palanqueeId: number, diveId: number | null): Promise<void> =>
    api.put(`${BASE}/${id}/dives/assign`, { palanqueeId, diveId }).then(() => undefined),

  // ── Palanquées ──────────────────────────────────────────────────────────────

  listPalanquees: (id: number): Promise<FreePalanquee[]> =>
    api.get<FreePalanquee[]>(`${BASE}/${id}/palanquees`).then(r => r.data),

  createPalanquee: (id: number, name: string): Promise<FreePalanquee> =>
    api.post<FreePalanquee>(`${BASE}/${id}/palanquees`, { name }).then(r => r.data),

  updatePalanquee: (id: number, pid: number, name: string, depth?: string, duration?: string): Promise<FreePalanquee> =>
    api.put<FreePalanquee>(`${BASE}/${id}/palanquees/${pid}`, { name, depth, duration }).then(r => r.data),

  deletePalanquee: (id: number, pid: number): Promise<void> =>
    api.delete(`${BASE}/${id}/palanquees/${pid}`).then(() => undefined),

  assignDiver: (id: number, diverId: number, palanqueeId: number | null, fromPalanqueeId?: number | null): Promise<void> =>
    api.put(`${BASE}/${id}/palanquees/assign`, { diverId, palanqueeId, fromPalanqueeId }).then(() => undefined),

  reorderPalanquee: (id: number, pid: number, diverIds: number[]): Promise<void> =>
    api.put(`${BASE}/${id}/palanquees/${pid}/reorder`, { diverIds }).then(() => undefined),

  updateMemberAptitudes: (id: number, pid: number, did: number, aptitudes?: string): Promise<void> =>
    api.patch(`${BASE}/${id}/palanquees/${pid}/members/${did}/aptitudes`, { aptitudes: aptitudes ?? null }).then(() => undefined),
};
