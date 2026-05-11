import { Fragment, useState, useEffect, useRef, useCallback } from 'react';
import { freeSessionService } from '../services/freeSessionService';
import { adminService } from '../services/adminService';
import { exportDiverListCsv } from '../utils/exportDiverList';
import type { FreeDiveSession, FreeSessionDiver, FreePalanquee, FreeSessionDive, SlotDiver, Palanquee, DiveSlot, UserSearchResult } from '../types';

// ── constantes ──────────────────────────────────────────────────────────────
const LEVEL_COLORS: Record<string, string> = {
  'E1': '#93c5fd', 'E2': '#60a5fa',
  'Niveau 1': '#3b82f6', 'Niveau 2': '#10b981',
  'Niveau 3': '#f59e0b', 'Niveau 4': '#8b5cf6',
  'Guide de Palanquée': '#0ea5e9',
  'MF1': '#ef4444', 'MF2': '#dc2626',
  'Moniteur': '#ef4444', 'Directeur de plongée': '#991b1b',
  'PADI Open Water': '#3b82f6', 'PADI Advanced': '#10b981',
  'PADI Rescue': '#f59e0b',
  'Prepa-N1': '#bfdbfe', 'Prepa-N2': '#a7f3d0',
  'Prepa-N3': '#fde68a', 'Prepa-N4': '#ddd6fe',
  'Prepa-MF1': '#fca5a5', 'Prepa-MF2': '#f87171',
};

function getLevelColor(level: string) { return LEVEL_COLORS[level] ?? '#6b7280'; }
function fmtDate(d: string) { const [y, m, day] = d.split('-'); return `${day}/${m}/${y}`; }

const APTITUDES_OPTIONS = ['PE12','PE20','PE40','PE60','PA12','PA20','PA40','PA60','E1','E2','E3','E4','GP'];
const DEPTH_OPTIONS = ['6m', '12m', '20m', '30m', '40m', '50m', '60m'];
const DURATION_OPTIONS = Array.from({ length: 24 }, (_, i) => `${(i + 1) * 10}'`);
const LEVEL_OPTIONS = Object.keys(LEVEL_COLORS);

// ── Conversion FreePalanquee → shape attendu par les helpers ─────────────────
function toPalanquee(p: FreePalanquee): Palanquee {
  return { ...p, slotDiveId: p.diveId ?? null } as unknown as Palanquee;
}

// ── DiverCard ────────────────────────────────────────────────────────────────
interface DiverCardProps {
  diver: FreeSessionDiver;
  onDragStart: (id: number) => void;
  onDragEnter: () => void;
  isDragging: boolean;
  onLevelChange: (id: number, level: string) => void;
  onAptitudesChange: (id: number, aptitudes: string) => void;
  onTap?: (id: number) => void;
  isPicked?: boolean;
  aptitudesOptions?: string[];
  isReadOnly?: boolean;
}

function DiverCard({ diver, onDragStart, onDragEnter, isDragging, onLevelChange, onAptitudesChange, onTap, isPicked, aptitudesOptions, isReadOnly }: DiverCardProps) {
  const [editingLevel, setEditingLevel] = useState(false);
  const [editingAptitudes, setEditingAptitudes] = useState(false);
  const color = getLevelColor(diver.level);
  const levelOptions = [...LEVEL_OPTIONS];
  if (diver.level && !levelOptions.includes(diver.level)) levelOptions.unshift(diver.level);
  const isEditing = editingLevel || editingAptitudes;
  const aptOpts = aptitudesOptions ?? APTITUDES_OPTIONS;

  return (
    <div
      className={`palanquee-postit${isDragging ? ' palanquee-postit--dragging' : ''}${isPicked ? ' palanquee-postit--picked' : ''}${isReadOnly ? ' palanquee-postit--readonly' : ''}`}
      draggable={!isEditing && !onTap && !isReadOnly}
      onDragStart={e => { if (isEditing || isReadOnly) { e.preventDefault(); return; } e.dataTransfer.effectAllowed = 'move'; onDragStart(diver.id); }}
      onDragEnter={e => { e.stopPropagation(); if (!isEditing && !isReadOnly) onDragEnter(); }}
      onClick={() => !isReadOnly && onTap?.(diver.id)}
      style={{ borderTop: `4px solid ${color}` }}
      title={isReadOnly ? 'Vue lecture seule' : onTap ? 'Appuyer pour sélectionner' : 'Glisser pour déplacer'}
    >
      <div className="palanquee-postit-name">
        {diver.isDirector && <span className="palanquee-postit-director" title="Directeur de plongée">🎖 </span>}
        {diver.firstName} {diver.lastName}
      </div>
      {editingLevel ? (
        <select autoFocus className="palanquee-postit-level-select" value={diver.level}
          onBlur={() => setEditingLevel(false)}
          onChange={e => { onLevelChange(diver.id, e.target.value); setEditingLevel(false); }}
          onMouseDown={e => e.stopPropagation()} onClick={e => e.stopPropagation()}>
          {levelOptions.map(l => <option key={l} value={l}>{l}</option>)}
        </select>
      ) : (
        <div className="palanquee-postit-level" style={{ color }}
          title="Double-clic pour modifier le niveau"
          onDoubleClick={e => { if (isReadOnly) return; e.stopPropagation(); setEditingLevel(true); }}>
          {diver.level}
        </div>
      )}
      <div className="palanquee-postit-apt-wrapper">
        {editingAptitudes ? (
          <select
            autoFocus
            className="palanquee-postit-level-select"
            value={diver.aptitudes ?? ''}
            onBlur={() => setEditingAptitudes(false)}
            onChange={e => { onAptitudesChange(diver.id, e.target.value); setEditingAptitudes(false); }}
            onMouseDown={e => e.stopPropagation()}
            onClick={e => e.stopPropagation()}
          >
            <option value="">— aucune —</option>
            {aptOpts.map(a => <option key={a} value={a}>{a}</option>)}
          </select>
        ) : (
          <div className="palanquee-postit-aptitudes">
            {diver.aptitudes
              ? diver.aptitudes
              : <span className="palanquee-postit-aptitudes--empty">aptitudes</span>}
            <button
              className="palanquee-postit-apt-edit-icon"
              title="Modifier les aptitudes"
              onClick={e => { if (isReadOnly) return; e.stopPropagation(); setEditingAptitudes(true); }}
              onMouseDown={e => e.stopPropagation()}
            >✎</button>
          </div>
        )}
      </div>
    </div>
  );
}

// ── DropZone ─────────────────────────────────────────────────────────────────
interface DropZoneProps {
  palanqueeId: number | null;
  label: string;
  labelIcon?: string;
  divers: FreeSessionDiver[];
  draggedId: number | null;
  onDragStart: (id: number, fromId: number | null) => void;
  onDragEnterCard: (palId: number | null, before: number) => void;
  onDragEnterEnd: (palId: number | null) => void;
  onDrop: (palId: number | null) => void;
  onLevelChange: (id: number, level: string) => void;
  onAptitudesChange: (id: number, aptitudes: string) => void;
  onTapDiver?: (id: number) => void;
  mobilePickedId?: number | null;
  insertBeforeId?: number | null;
  isUnassigned?: boolean;
  isPool?: boolean;
  palanqueeIndex?: number;
  aptitudesOptions?: string[];
  isReadOnly?: boolean;
}

function DropZone({ palanqueeId, label, labelIcon, divers, draggedId, onDragStart, onDragEnterCard, onDragEnterEnd, onDrop, onLevelChange, onAptitudesChange, onTapDiver, mobilePickedId, insertBeforeId, isUnassigned, isPool, palanqueeIndex, aptitudesOptions, isReadOnly }: DropZoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);
  const handleDragOver = (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(true); };
  const handleDragLeave = () => setIsDragOver(false);
  const handleDrop = (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(false); onDrop(palanqueeId); };

  return (
    <div
      className={`palanquee-dropzone${isDragOver ? ' palanquee-dropzone--over' : ''}${isUnassigned ? ' palanquee-dropzone--unassigned' : ''}`}
      onDragOver={handleDragOver} onDragLeave={handleDragLeave} onDrop={handleDrop}
    >
      <div className="palanquee-dropzone-header">
        <span className="palanquee-dropzone-icon">{labelIcon}</span>
        <span className="palanquee-dropzone-label">
          {palanqueeIndex !== undefined ? `P${palanqueeIndex} – ` : ''}{label}
        </span>
        <span className="palanquee-dropzone-count">{divers.length}</span>
      </div>
      <div className={`palanquee-cards-area${isPool ? ' palanquee-cards-area--pool' : ''}`} onDragEnter={() => onDragEnterEnd(palanqueeId)}>
        {divers.map(d => (
          <Fragment key={d.id}>
            {insertBeforeId !== undefined && insertBeforeId === d.id && <div className="palanquee-insert-line" />}
            <DiverCard
              diver={d}
              onDragStart={id => onDragStart(id, palanqueeId)}
              onDragEnter={() => onDragEnterCard(palanqueeId, d.id)}
              isDragging={d.id === draggedId}
              onLevelChange={onLevelChange}
              onAptitudesChange={onAptitudesChange}
              onTap={onTapDiver}
              isPicked={mobilePickedId === d.id}
              aptitudesOptions={aptitudesOptions}
              isReadOnly={isReadOnly}
            />
          </Fragment>
        ))}
        {insertBeforeId !== undefined && insertBeforeId === null && <div className="palanquee-insert-line" />}
        {divers.length === 0 && <div className="palanquee-empty-hint">Déposer un plongeur ici</div>}
      </div>
    </div>
  );
}

// ── Panneau d'ajout de plongeur (pattern SlotBlock) ───────────────────────────
interface AddDiverPanelProps {
  onSave: (req: Omit<FreeSessionDiver, 'id'>) => void;
  onCancel: () => void;
  levels: string[];
  hasDirector?: boolean;
}

function AddDiverPanel({ onSave, onCancel, levels, hasDirector }: AddDiverPanelProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [level, setLevel] = useState(levels[0] ?? '');
  const [isDirector, setIsDirector] = useState(false);

  // Debounce de la recherche
  useEffect(() => {
    if (!searchQuery.trim()) { setSearchResults([]); return; }
    const t = setTimeout(async () => {
      setSearching(true);
      try { setSearchResults(await adminService.searchUsers(searchQuery)); }
      catch { setSearchResults([]); }
      finally { setSearching(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  const selectUser = (u: UserSearchResult) => {
    setFirstName(u.firstName);
    setLastName(u.lastName);
    setSearchQuery('');
    setSearchResults([]);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !level) return;
    onSave({ firstName, lastName, level, isDirector });
    setFirstName(''); setLastName(''); setIsDirector(false);
    setSearchQuery(''); setSearchResults([]);
  };

  return (
    <form className="diver-form" onSubmit={handleSubmit} style={{ marginBottom: 12 }}>
      {/* Recherche utilisateur existant */}
      <div className="user-search-wrapper">
        <input
          className="user-search-input"
          placeholder="🔍 Rechercher un utilisateur…"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          autoComplete="off"
          autoFocus
        />
        {searching && <div className="user-search-hint">Recherche…</div>}
        {!searching && searchResults.length > 0 && (
          <ul className="user-search-results">
            {searchResults.map(u => (
              <li key={u.id} className="user-search-item" onMouseDown={() => selectUser(u)}>
                <span className="user-search-name">{u.name}</span>
                <span className="user-search-email">{u.email}</span>
              </li>
            ))}
          </ul>
        )}
        {!searching && searchQuery.trim() && searchResults.length === 0 && (
          <div className="user-search-hint">Aucun utilisateur trouvé</div>
        )}
      </div>

      <div className="diver-form-row">
        <input placeholder="Prénom *" value={firstName} required
          onChange={e => setFirstName(e.target.value)} />
        <input placeholder="Nom *" value={lastName} required
          onChange={e => setLastName(e.target.value)} />
      </div>

      <select value={level} onChange={e => setLevel(e.target.value)}>
        {levels.map(l => <option key={l} value={l}>{l}</option>)}
      </select>

      <div className="diver-director-toggle-row">
        <label className="diver-director-checkbox" title={hasDirector ? 'Un directeur de plongée est déjà désigné' : undefined}>
          <input type="checkbox" checked={isDirector}
            disabled={hasDirector && !isDirector}
            onChange={e => setIsDirector(e.target.checked)} />
          <span>🎖 Directeur de plongée{hasDirector && !isDirector ? ' (déjà désigné)' : ''}</span>
        </label>
      </div>

      <div className="diver-form-actions">
        <button type="submit" className="btn-diver-save"
          disabled={!firstName.trim() || !lastName.trim() || !level}>
          Ajouter
        </button>
        <button type="button" className="btn-diver-cancel" onClick={onCancel}>Annuler</button>
      </div>
    </form>
  );
}

// ── Formulaire d'édition plongeur (avec recherche) ───────────────────────────
interface EditDiverFormProps {
  initial: FreeSessionDiver;
  onSave: (req: Omit<FreeSessionDiver, 'id'>) => void;
  onCancel: () => void;
  levels: string[];
  hasDirector?: boolean;
}

function EditDiverForm({ initial, onSave, onCancel, levels, hasDirector }: EditDiverFormProps) {
  const [firstName, setFirstName] = useState(initial.firstName);
  const [lastName, setLastName] = useState(initial.lastName);
  const [level, setLevel] = useState(initial.level);
  const [isDirector, setIsDirector] = useState(initial.isDirector ?? false);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    if (!searchQuery.trim()) { setSearchResults([]); return; }
    const t = setTimeout(async () => {
      setSearching(true);
      try { setSearchResults(await adminService.searchUsers(searchQuery)); }
      catch { setSearchResults([]); }
      finally { setSearching(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  const selectUser = (u: UserSearchResult) => {
    setFirstName(u.firstName);
    setLastName(u.lastName);
    setSearchQuery('');
    setSearchResults([]);
  };

  const lvlOptions = [...levels];
  if (initial.level && !lvlOptions.includes(initial.level)) lvlOptions.unshift(initial.level);

  return (
    <form className="diver-form" onSubmit={e => { e.preventDefault(); if (!firstName.trim() || !lastName.trim() || !level) return; onSave({ ...initial, firstName, lastName, level, isDirector }); }} style={{ marginBottom: 12 }}>
      <div className="user-search-wrapper">
        <input
          className="user-search-input"
          placeholder="🔍 Rechercher un utilisateur…"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          autoComplete="off"
        />
        {searching && <div className="user-search-hint">Recherche…</div>}
        {!searching && searchResults.length > 0 && (
          <ul className="user-search-results">
            {searchResults.map(u => (
              <li key={u.id} className="user-search-item" onMouseDown={() => selectUser(u)}>
                <span className="user-search-name">{u.name}</span>
                <span className="user-search-email">{u.email}</span>
              </li>
            ))}
          </ul>
        )}
        {!searching && searchQuery.trim() && searchResults.length === 0 && (
          <div className="user-search-hint">Aucun utilisateur trouvé</div>
        )}
      </div>
      <div className="diver-form-row">
        <input placeholder="Prénom *" value={firstName} required onChange={e => setFirstName(e.target.value)} />
        <input placeholder="Nom *" value={lastName} required onChange={e => setLastName(e.target.value)} />
      </div>
      <select value={level} onChange={e => setLevel(e.target.value)}>
        {lvlOptions.map(l => <option key={l} value={l}>{l}</option>)}
      </select>
      <div className="diver-director-toggle-row">
        <label className="diver-director-checkbox" title={hasDirector && !initial.isDirector ? 'Un directeur de plongée est déjà désigné' : undefined}>
          <input type="checkbox" checked={isDirector}
            disabled={hasDirector && !initial.isDirector}
            onChange={e => setIsDirector(e.target.checked)} />
          <span>🎖 Directeur de plongée{hasDirector && !initial.isDirector ? ' (déjà désigné)' : ''}</span>
        </label>
      </div>
      <div className="diver-form-actions">
        <button type="submit" className="btn-diver-save">✓ Enregistrer</button>
        <button type="button" className="btn-diver-cancel" onClick={onCancel}>Annuler</button>
      </div>
    </form>
  );
}

// ── FreeSessionPage ──────────────────────────────────────────────────────────
interface Props {
  sessionId: number;
  onBack: () => void;
}

export function FreeSessionPage({ sessionId, onBack }: Props) {
  const [session, setSession]       = useState<FreeDiveSession | null>(null);
  const [allDivers, setAllDivers]   = useState<FreeSessionDiver[]>([]);
  const [palanquees, setPalanquees] = useState<FreePalanquee[]>([]);
  const [dives, setDives]           = useState<FreeSessionDive[]>([]);
  const [activeDiveId, setActiveDiveId] = useState<number | null>(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [saving, setSaving]         = useState(false);
  const [exporting, setExporting]   = useState(false);

  // drag
  const [draggedId, setDraggedId]   = useState<number | null>(null);
  const [draggedFromPalId, setDraggedFromPalId] = useState<number | null>(null);
  const [insertTarget, setInsertTarget] = useState<{ palId: number | null; beforeId: number | null } | null>(null);
  const insertRef = useRef(insertTarget);
  insertRef.current = insertTarget;

  // mobile
  const [isMobile, setIsMobile] = useState(() => window.matchMedia('(pointer: coarse), (max-width: 768px)').matches);
  const [mobilePickedId, setMobilePickedId] = useState<number | null>(null);
  const [mobileFromPalId, setMobileFromPalId] = useState<number | null>(null);
  const [activePalIdx, setActivePalIdx] = useState(0);

  // renaming
  const [renamingId, setRenamingId]   = useState<number | null>(null);
  const [renameDraft, setRenameDraft] = useState('');
  const renameInputRef = useRef<HTMLInputElement>(null);

  // plongeur form
  const [editingDiver, setEditingDiver]   = useState<FreeSessionDiver | null>(null);

  // aptitudes
  const [aptitudesOptions, setAptitudesOptions] = useState<string[]>(APTITUDES_OPTIONS);
  const [allLevels, setAllLevels] = useState<string[]>(LEVEL_OPTIONS);

  const boardRef = useRef<HTMLDivElement>(null);

  // ── chargement ────────────────────────────────────────────────────────────
  const loadAll = useCallback(async () => {
    setError('');
    try {
      const [diversData, palData, divesData] = await Promise.all([
        freeSessionService.listDivers(sessionId),
        freeSessionService.listPalanquees(sessionId),
        freeSessionService.listDives(sessionId),
      ]);
      setAllDivers(diversData);
      setPalanquees(palData);
      setDives(divesData);
    } catch {
      setError('Impossible de charger les données de la session.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  // Charge la session courante depuis la liste
  useEffect(() => {
    freeSessionService.list().then(list => {
      const s = list.find(x => x.id === sessionId) ?? null;
      setSession(s);
    }).catch(() => {});
  }, [sessionId]);

  useEffect(() => { loadAll(); }, [loadAll]);

  useEffect(() => {
    adminService.getConfig().then(cfg => {
      if (cfg.aptitudes?.length) setAptitudesOptions(cfg.aptitudes);
      const lvls = [...(cfg.levels ?? []), ...(cfg.dpLevels ?? [])];
      if (lvls.length) setAllLevels([...new Set(lvls)]);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    const mq = window.matchMedia('(pointer: coarse), (max-width: 768px)');
    const h = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mq.addEventListener('change', h);
    return () => mq.removeEventListener('change', h);
  }, []);

  useEffect(() => {
    if (palanquees.length > 0 && activePalIdx >= palanquees.length) setActivePalIdx(palanquees.length - 1);
  }, [palanquees.length, activePalIdx]);

  // Sélectionner automatiquement la première plongée si aucune n'est sélectionnée ou valide.
  // Supprime le concept de vue « Toutes » : en mode multi-plongées, une plongée est toujours active.
  useEffect(() => {
    if (dives.length === 0) {
      if (activeDiveId !== null) setActiveDiveId(null);
      return;
    }
    if (activeDiveId === null || !dives.some(d => d.id === activeDiveId)) {
      setActiveDiveId(dives[0].id);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dives]);

  useEffect(() => { if (renamingId !== null) renameInputRef.current?.focus(); }, [renamingId]);

  // auto-scroll horizontal pendant drag
  useEffect(() => {
    const board = boardRef.current;
    if (!board) return;
    const EDGE = 80; const MAX_SPEED = 16;
    let dir = 0; let speed = 0; let frame: number | null = null;
    const tick = () => { if (dir !== 0) { board.scrollLeft += speed * dir; frame = requestAnimationFrame(tick); } else frame = null; };
    const over = (e: DragEvent) => {
      const rect = board.getBoundingClientRect(); const x = e.clientX - rect.left;
      if (x < EDGE) { dir = -1; speed = Math.round(MAX_SPEED * (1 - x / EDGE)); }
      else if (x > rect.width - EDGE) { dir = 1; speed = Math.round(MAX_SPEED * (1 - (rect.width - x) / EDGE)); }
      else dir = 0;
      if (dir !== 0 && frame === null) frame = requestAnimationFrame(tick);
    };
    const stop = () => { dir = 0; if (frame !== null) { cancelAnimationFrame(frame); frame = null; } };
    board.addEventListener('dragover', over);
    board.addEventListener('dragleave', stop);
    board.addEventListener('drop', stop);
    board.addEventListener('dragend', stop);
    return () => { board.removeEventListener('dragover', over); board.removeEventListener('dragleave', stop); board.removeEventListener('drop', stop); board.removeEventListener('dragend', stop); stop(); };
  }, []);

  // ── vues filtrées ─────────────────────────────────────────────────────────
  const filteredPals = palanquees.filter(p => p.diveId === activeDiveId);
  // La vue « Toutes » a été supprimée : en mode multi-plongées une plongée est toujours active.
  // isOverviewReadOnly est conservé par sécurité mais vaut toujours false.
  const isOverviewReadOnly = false;
  const assignedIds = new Set(filteredPals.flatMap(p => p.divers.map(d => d.id)));
  const unassigned = allDivers.filter(d => !assignedIds.has(d.id));

  // ── assignation commune ───────────────────────────────────────────────────
  const handleAssign = useCallback(async (
    diverId: number,
    targetPalId: number | null,
    beforeId: number | null = null,
    fromPalId?: number | null,
  ) => {
    const ctxPals = filteredPals;
    const currentPalId = fromPalId !== undefined ? fromPalId : (ctxPals.find(p => p.divers.some(d => d.id === diverId))?.id ?? null);
    const diver = allDivers.find(d => d.id === diverId);
    if (!diver) return;

    if (currentPalId === targetPalId && targetPalId !== null) {
      const pal = palanquees.find(p => p.id === targetPalId)!;
      const without = pal.divers.filter(d => d.id !== diverId);
      const reordered = beforeId === null ? [...without, diver] : (() => { const i = without.findIndex(d => d.id === beforeId); return i === -1 ? [...without, diver] : [...without.slice(0, i), diver, ...without.slice(i)]; })();
      setPalanquees(prev => prev.map(p => p.id === targetPalId ? { ...p, divers: reordered } : p));
      try { await freeSessionService.reorderPalanquee(sessionId, targetPalId, reordered.map(d => d.id)); } catch { await loadAll(); }
      return;
    }

    const targetDivers = palanquees.find(p => p.id === targetPalId)?.divers ?? [];
    const without = targetDivers.filter(d => d.id !== diverId);
    const optimistic = beforeId === null ? [...without, diver] : (() => { const i = without.findIndex(d => d.id === beforeId); return i === -1 ? [...without, diver] : [...without.slice(0, i), diver, ...without.slice(i)]; })();

    setPalanquees(prev => prev.map(p => {
      if (p.id === currentPalId) return { ...p, divers: p.divers.filter(d => d.id !== diverId) };
      if (p.id === targetPalId)  return { ...p, divers: optimistic };
      return p;
    }));
    try {
      await freeSessionService.assignDiver(sessionId, diverId, targetPalId, currentPalId);
      if (targetPalId !== null) await freeSessionService.reorderPalanquee(sessionId, targetPalId, optimistic.map(d => d.id));
    } catch { await loadAll(); }
  }, [allDivers, palanquees, activeDiveId, sessionId, loadAll]);

  // ── drag & drop ───────────────────────────────────────────────────────────
  const handleDragStart = (id: number, fromId: number | null = null) => { setDraggedId(id); setDraggedFromPalId(fromId); setInsertTarget(null); };
  const handleDragEnterCard = (palId: number | null, before: number) => setInsertTarget({ palId, beforeId: before });
  const handleDragEnterEnd = (palId: number | null) => setInsertTarget({ palId, beforeId: null });
  const handleDrop = async (targetPalId: number | null) => {
    if (draggedId === null) return;
    const t = insertRef.current;
    const did = draggedId; const fromId = draggedFromPalId;
    setDraggedId(null); setDraggedFromPalId(null); setInsertTarget(null);
    const before = t?.palId === targetPalId ? t.beforeId : null;
    await handleAssign(did, targetPalId, before, fromId);
  };

  // ── tap mobile ────────────────────────────────────────────────────────────
  const handleMobilePick = (id: number, fromId: number | null = null) => {
    if (mobilePickedId === id) { setMobilePickedId(null); setMobileFromPalId(null); }
    else { setMobilePickedId(id); setMobileFromPalId(fromId); }
  };
  const handleMobileAssign = async (targetPalId: number | null) => {
    if (mobilePickedId === null) return;
    const did = mobilePickedId; const fromId = mobileFromPalId;
    setMobilePickedId(null); setMobileFromPalId(null);
    await handleAssign(did, targetPalId, null, fromId);
  };

  // ── level / aptitudes inline ──────────────────────────────────────────────
  const handleLevelChange = useCallback(async (diverId: number, newLevel: string) => {
    const d = allDivers.find(x => x.id === diverId);
    if (!d || d.level === newLevel) return;
    const upd = (x: FreeSessionDiver) => x.id === diverId ? { ...x, level: newLevel } : x;
    setAllDivers(prev => prev.map(upd));
    setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.map(upd) })));
    try { await freeSessionService.updateDiver(sessionId, diverId, { ...d, level: newLevel }); } catch { await loadAll(); }
  }, [allDivers, sessionId, loadAll]);

  const handleAptitudesChange = useCallback(async (diverId: number, newApt: string) => {
    const palInDive = activeDiveId !== null ? palanquees.find(p => p.diveId === activeDiveId && p.divers.some(d => d.id === diverId)) : null;
    if (palInDive) {
      setPalanquees(prev => prev.map(p => p.id === palInDive.id ? { ...p, divers: p.divers.map(d => d.id === diverId ? { ...d, aptitudes: newApt || undefined } : d) } : p));
      try { await freeSessionService.updateMemberAptitudes(sessionId, palInDive.id, diverId, newApt || undefined); } catch { await loadAll(); }
    } else {
      const d = allDivers.find(x => x.id === diverId);
      if (!d) return;
      const upd = (x: FreeSessionDiver) => x.id === diverId ? { ...x, aptitudes: newApt || undefined } : x;
      setAllDivers(prev => prev.map(upd));
      setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.map(upd) })));
      try { await freeSessionService.updateDiver(sessionId, diverId, { ...d, aptitudes: newApt || undefined }); } catch { await loadAll(); }
    }
  }, [allDivers, palanquees, activeDiveId, sessionId, loadAll]);

  // ── palanquées ────────────────────────────────────────────────────────────
  const handleAddPalanquee = async () => {
    setSaving(true);
    try {
      const name = `Palanquée ${palanquees.length + 1}`;
      const created = await freeSessionService.createPalanquee(sessionId, name);
      if (activeDiveId !== null) {
        await freeSessionService.assignPalanqueeToDive(sessionId, created.id, activeDiveId);
        created.diveId = activeDiveId;
      }
      setPalanquees(prev => [...prev, created]);
    } catch { setError('Impossible de créer la palanquée.'); } finally { setSaving(false); }
  };

  const handleDeletePalanquee = async (id: number) => {
    const p = palanquees.find(x => x.id === id);
    if (!p) return;
    if (p.divers.length > 0 && !window.confirm(`Supprimer « ${p.name} » et désassigner ses ${p.divers.length} plongeur(s) ?`)) return;
    try { await freeSessionService.deletePalanquee(sessionId, id); setPalanquees(prev => prev.filter(x => x.id !== id)); } catch { setError('Impossible de supprimer la palanquée.'); }
  };

  const commitRename = async (id: number) => {
    const draft = renameDraft.trim();
    if (!draft) { setRenamingId(null); return; }
    const pal = palanquees.find(p => p.id === id);
    if (draft === pal?.name) { setRenamingId(null); return; }
    setPalanquees(ps => ps.map(p => p.id === id ? { ...p, name: draft } : p));
    setRenamingId(null);
    try { await freeSessionService.updatePalanquee(sessionId, id, draft, pal?.depth, pal?.duration); } catch { await loadAll(); }
  };

  const handleDepthChange = useCallback(async (palId: number, depth: string) => {
    const pal = palanquees.find(p => p.id === palId); if (!pal) return;
    setPalanquees(prev => prev.map(p => p.id === palId ? { ...p, depth: depth || undefined } : p));
    try { await freeSessionService.updatePalanquee(sessionId, palId, pal.name, depth || undefined, pal.duration); } catch { await loadAll(); }
  }, [palanquees, sessionId, loadAll]);

  const handleDurationChange = useCallback(async (palId: number, duration: string) => {
    const pal = palanquees.find(p => p.id === palId); if (!pal) return;
    setPalanquees(prev => prev.map(p => p.id === palId ? { ...p, duration: duration || undefined } : p));
    try { await freeSessionService.updatePalanquee(sessionId, palId, pal.name, pal.depth, duration || undefined); } catch { await loadAll(); }
  }, [palanquees, sessionId, loadAll]);

  // ── plongées multiples ────────────────────────────────────────────────────
  const handleAddDive = async () => {
    setSaving(true);
    try {
      const n = dives.length + 1;
      const created = await freeSessionService.createDive(sessionId, { label: `Plongée ${n}`, startTime: session?.startTime ?? null });
      // Première plongée créée : assigner automatiquement toutes les palanquées existantes
      if (dives.length === 0 && palanquees.length > 0) {
        await Promise.all(
          palanquees.map(p => freeSessionService.assignPalanqueeToDive(sessionId, p.id, created.id))
        );
        setPalanquees(prev => prev.map(p => ({ ...p, diveId: created.id })));
      }
      setDives(prev => [...prev, created]);
      setActiveDiveId(created.id);
    } catch { setError('Impossible de créer la plongée.'); } finally { setSaving(false); }
  };

  const handleDeleteDive = async (diveId: number) => {
    const dive = dives.find(d => d.id === diveId);
    const label = dive?.label ?? `Plongée ${dive?.diveIndex}`;
    const isLast = dives.length === 1;
    
    if (isLast) {
      if (!window.confirm(`Supprimer « ${label} » ? C'est la dernière plongée — l'organisation reviendra en mode plongée unique. Les palanquées existantes seront conservées.`)) return;
    } else {
      const palanqueesForDive = palanquees.filter(p => p.diveId === diveId);
      if (palanqueesForDive.length > 0) {
        if (!window.confirm(`Supprimer « ${label} » ? Ses ${palanqueesForDive.length} palanquée(s) seront désassociées mais conservées.`)) return;
      } else {
        if (!window.confirm(`Supprimer « ${label} » ?`)) return;
      }
    }
    
    try {
      await freeSessionService.deleteDive(sessionId, diveId);
      setDives(prev => prev.filter(d => d.id !== diveId).map((d, i) => ({ ...d, diveIndex: i + 1 })));
      if (activeDiveId === diveId) setActiveDiveId(null);
      // Si c'était la dernière plongée, recharger les palanquées depuis le serveur
      // (le backend a détaché les palanquées)
      if (isLast) {
        await loadAll();
      } else {
        setPalanquees(prev => prev.map(p => p.diveId === diveId ? { ...p, diveId: null } : p));
      }
    } catch {
      setError('Impossible de supprimer la plongée.');
    } finally {
      setSaving(false);
    }
  };

  const handleDiveTimeChange = useCallback(async (diveId: number, field: 'startTime' | 'endTime', value: string) => {
    const dive = dives.find(d => d.id === diveId); if (!dive) return;
    const updated = { ...dive, [field]: value || null };
    setDives(prev => prev.map(d => d.id === diveId ? updated : d));
    try { await freeSessionService.updateDive(sessionId, diveId, { label: dive.label, startTime: field === 'startTime' ? (value || null) : dive.startTime, endTime: field === 'endTime' ? (value || null) : dive.endTime, depth: dive.depth, duration: dive.duration }); } catch { await loadAll(); }
  }, [dives, sessionId, loadAll]);

  // ── plongeurs ─────────────────────────────────────────────────────────────
  const handleAddDiver = async (req: Omit<FreeSessionDiver, 'id'>) => {
    try {
      const created = await freeSessionService.addDiver(sessionId, req);
      setAllDivers(prev => [...prev, created]);
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m ?? 'Impossible d\'ajouter le plongeur.');
    }
  };

  const handleUpdateDiver = async (req: Omit<FreeSessionDiver, 'id'>) => {
    if (!editingDiver) return;
    try {
      const updated = await freeSessionService.updateDiver(sessionId, editingDiver.id, req);
      const upd = (d: FreeSessionDiver) => d.id === editingDiver.id ? { ...updated } : d;
      setAllDivers(prev => prev.map(upd));
      setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.map(upd) })));
      setEditingDiver(null);
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m ?? 'Impossible de modifier le plongeur.');
    }
  };

  const handleRemoveDiver = async (diverId: number) => {
    const d = allDivers.find(x => x.id === diverId);
    if (!d) return;
    if (!window.confirm(`Retirer ${d.firstName} ${d.lastName} de la session ?`)) return;
    try {
      await freeSessionService.removeDiver(sessionId, diverId);
      setAllDivers(prev => prev.filter(x => x.id !== diverId));
      setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.filter(x => x.id !== diverId) })));
    } catch { setError('Impossible de supprimer le plongeur.'); }
  };

  // ── export ────────────────────────────────────────────────────────────────
  const buildFreeSessionFilename = (ext: string) => {
    const date = session?.diveDate ?? '';
    const time = (session?.startTime ?? '').replace(':', '-');
    const slug = (session?.label ?? 'organisation')
      .trim()
      .replace(/[^a-zA-Z0-9\u00C0-\u024F]+/g, '-')
      .replace(/^-|-$/g, '');
    return `${date}-${time}-${slug}.${ext}`;
  };

  const handleExportDiverList = () => {
    if (!session) return;
    // Adapter session → DiveSlot minimal pour exportDiverListCsv
    const fakeSlot = { id: session.id, slotDate: session.diveDate, startTime: session.startTime, endTime: '23:59', title: session.label, club: null } as DiveSlot;
    exportDiverListCsv(fakeSlot, allDivers as unknown as SlotDiver[], buildFreeSessionFilename('csv'));
  };

  const handleExportExcel = async () => {
    if (!session) return;
    setExporting(true);
    try {
      const { exportFicheSecuriteAvecPalanquees } = await import('../utils/exportFicheSecuriteAvecPalanquees');
      const fakeSlot = {
        id: session.id, slotDate: session.diveDate,
        startTime: session.startTime, endTime: session.startTime,
        title: session.label ?? '', club: null, diverCount: 999,
        divers: [], notes: null, slotType: null, createdById: 0, createdByName: '',
        registrationOpen: false, registrationOpensAt: null, requiresAttachments: false,
        hasSafetySheets: false, waitingListCount: 0,
      } satisfies DiveSlot;
      if (dives.length > 0) {
        // Mode multi-plongées : exporter la liste globale des plongeurs (sans palanquées spécifiques)
        // Chaque plongée a son propre bouton dans la barre d'horaire pour sa fiche détaillée
        await exportFicheSecuriteAvecPalanquees(fakeSlot, allDivers as unknown as SlotDiver[], [], undefined, undefined, undefined, buildFreeSessionFilename('xlsx'), session.label ?? undefined);
      } else {
        // Mode plongée unique : exporter avec toutes les palanquées
        const fakePals = palanquees.map(toPalanquee);
        await exportFicheSecuriteAvecPalanquees(fakeSlot, allDivers as unknown as SlotDiver[], fakePals, undefined, undefined, undefined, buildFreeSessionFilename('xlsx'), session.label ?? undefined);
      }
    } catch (err) {
      console.error('Export fiche de sécurité :', err);
    } finally {
      setExporting(false);
    }
  };

  // ── rendu ─────────────────────────────────────────────────────────────────
  if (loading) return <div className="palanquee-page"><div className="palanquee-loading">Chargement…</div></div>;

  return (
    <div className="palanquee-page">
      {/* En-tête */}
      <div className="palanquee-page-header">
        <button className="palanquee-back-btn" onClick={onBack}>← Retour</button>
        <div className="palanquee-page-title-block">
          <h2 className="palanquee-page-title">🧩 Organisation libre <span style={{ fontSize: 11, fontWeight: 600, background: '#f59e0b', color: '#fff', borderRadius: 4, padding: '1px 6px', verticalAlign: 'middle', letterSpacing: '0.05em' }}>BETA</span></h2>
          {session && (
            <div className="palanquee-page-subtitle">
              <span>{fmtDate(session.diveDate)}</span>
              <span className="palanquee-separator">·</span>
              <span>{session.startTime.slice(0, 5)}</span>
              {session.label && <><span className="palanquee-separator">·</span><span>{session.label}</span></>}
              <span className="palanquee-separator">·</span>
              <span>🤿 {allDivers.length} plongeur{allDivers.length !== 1 ? 's' : ''}</span>
            </div>
          )}
        </div>
        <div className="palanquee-page-actions">
          {!isOverviewReadOnly && (
            <button
              className="palanquee-add-btn"
              onClick={handleAddPalanquee}
              disabled={saving}
            >+ Palanquée</button>
          )}
          <button className="palanquee-export-btn" onClick={handleExportDiverList} disabled={allDivers.length === 0} title="Liste des plongeurs CSV">📋 Liste</button>
          <button className="palanquee-export-btn" onClick={handleExportExcel} disabled={exporting || allDivers.length === 0} title="Fiche de sécurité Excel">
            {exporting ? '…' : '📥 Fiche sécurité'}
          </button>
        </div>
      </div>

      {error && <div className="palanquee-error">{error}</div>}

      {/* Formulaire ajout plongeur (toujours visible, sauf en mode édition) */}
      {!editingDiver && (
        <div style={{ padding: '0 16px 8px' }}>
          <AddDiverPanel
            onSave={handleAddDiver}
            onCancel={() => {}}
            levels={allLevels}
            hasDirector={allDivers.some(d => d.isDirector)}
          />
        </div>
      )}

      {/* Formulaire édition plongeur */}
      {editingDiver && (
        <div style={{ padding: '0 16px 8px' }}>
          <EditDiverForm
            initial={editingDiver}
            onSave={handleUpdateDiver}
            onCancel={() => setEditingDiver(null)}
            levels={allLevels}
            hasDirector={allDivers.some(d => d.isDirector)}
          />
        </div>
      )}

      {/* Onglets plongées */}
      {dives.length > 0 && (
        <div className="dive-tabs">
          {dives.map(dive => (
            <span key={dive.id} className={`dive-tab-wrapper${activeDiveId === dive.id ? ' dive-tab-wrapper--active' : ''}`}>
              <button
                className={`dive-tab${activeDiveId === dive.id ? ' dive-tab--active' : ''}`}
                onClick={() => setActiveDiveId(dive.id)}
              >
                🤿 {dive.label ?? `Plongée ${dive.diveIndex}`}
              </button>
              <button
                className="dive-tab-delete"
                onClick={() => handleDeleteDive(dive.id)}
                title={`Supprimer ${dive.label ?? `Plongée ${dive.diveIndex}`}`}
              >✕</button>
            </span>
          ))}
          <button
            className="dive-tab dive-tab--add"
            onClick={handleAddDive}
            disabled={saving}
            title="Ajouter une plongée"
          >
            + Plongée
          </button>
        </div>
      )}
      {dives.length === 0 && (
        <div className="dive-tabs-empty">
          <button
            className="dive-tab dive-tab--add"
            onClick={handleAddDive}
            disabled={saving}
            title="Organiser cette session en plusieurs plongées"
          >
            + Organiser en plusieurs plongées
          </button>
        </div>
      )}

      {/* Horaire plongée active */}
      {activeDiveId !== null && dives.length > 0 && (() => {
        const dive = dives.find(d => d.id === activeDiveId);
        if (!dive) return null;
        return (
          <div className="dive-time-bar">
            <span className="dive-time-bar-label">🕐 Horaire :</span>
            <input
              type="time"
              className="dive-time-input"
              value={dive.startTime?.slice(0, 5) ?? ''}
              onChange={e => handleDiveTimeChange(dive.id, 'startTime', e.target.value)}
              title="Heure de début"
            />
            <span className="dive-time-bar-sep">—</span>
            <input
              type="time"
              className="dive-time-input"
              value={dive.endTime?.slice(0, 5) ?? ''}
              onChange={e => handleDiveTimeChange(dive.id, 'endTime', e.target.value)}
              title="Heure de fin"
            />
            <button
              className="palanquee-export-btn"
              disabled={exporting || allDivers.length === 0}
              title={`Exporter la fiche de sécurité — ${dive.label ?? `Plongée ${dive.diveIndex}`}`}
              onClick={async () => {
                if (!session) return;
                setExporting(true);
                try {
                  const { exportFicheSecuriteAvecPalanquees } = await import('../utils/exportFicheSecuriteAvecPalanquees');
                  const fakeSlot = {
                    id: session.id, slotDate: session.diveDate,
                    startTime: session.startTime, endTime: session.startTime,
                    title: session.label ?? '', club: null, diverCount: 999,
                    divers: [], notes: null, slotType: null, createdById: 0, createdByName: '',
                    registrationOpen: false, registrationOpensAt: null, requiresAttachments: false,
                    hasSafetySheets: false, waitingListCount: 0,
                  } satisfies DiveSlot;
                  const pals = palanquees.filter(p => p.diveId === dive.id).map(toPalanquee);
                  const diveLabel = dive.label ?? `Plongée ${dive.diveIndex}`;
                  await exportFicheSecuriteAvecPalanquees(
                    fakeSlot,
                    allDivers as unknown as SlotDiver[],
                    pals,
                    diveLabel,
                    dive.startTime ?? null,
                    dive.endTime ?? null,
                    buildFreeSessionFilename('xlsx'),
                    session.label ?? undefined,
                  );
                } catch (err) { console.error('Export fiche plongée :', err); }
                finally { setExporting(false); }
              }}
            >
              {exporting ? '…' : '📥 Fiche de sécurité'}
            </button>
          </div>
        );
      })()}

      {/* Panneau liste des plongeurs inscrits */}
      <div style={{ padding: '8px 16px', fontSize: 13, color: '#6b7280' }}>
        <details>
          <summary style={{ cursor: 'pointer', fontWeight: 600, color: '#374151' }}>
            👥 Plongeurs de la session ({allDivers.length})
          </summary>
          <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 4 }}>
            {allDivers.length === 0 && <p style={{ color: '#9ca3af' }}>Aucun plongeur. Cliquez sur "+ Plongeur" pour en ajouter.</p>}
            {allDivers.map(d => (
              <div key={d.id} style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#f9fafb', borderRadius: 6, padding: '4px 8px' }}>
                <span style={{ flex: 1, fontWeight: 500 }}>{d.isDirector ? '🎖 ' : ''}{d.firstName} {d.lastName}</span>
                <span style={{ color: getLevelColor(d.level), fontSize: 12 }}>{d.level}</span>
                {d.aptitudes && <span style={{ fontSize: 11, background: '#e0e7ff', padding: '1px 4px', borderRadius: 3 }}>{d.aptitudes}</span>}
                <button style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 14 }} title="Modifier" onClick={() => { setEditingDiver(d); }}>✏️</button>
                <button style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 14 }} title="Supprimer" onClick={() => handleRemoveDiver(d.id)}>🗑️</button>
              </div>
            ))}
          </div>
        </details>
      </div>

      {/* Board palanquées */}
      <div className="palanquee-board-wrapper">
        {isOverviewReadOnly && (
          <div className="palanquee-readonly-notice">
            👁️ Vue d'ensemble — sélectionnez une plongée pour modifier les assignations
          </div>
        )}

        {/* Pool non-assignés — sticky sur mobile, section normale sur desktop */}
        <div className={`palanquee-pool-section${isMobile ? ' palanquee-pool-section--sticky' : ''}`}>
          {isMobile ? (
            <div className="palanquee-dropzone palanquee-dropzone--unassigned">
              <div className="palanquee-dropzone-header">
                <span className="palanquee-dropzone-icon">🏊</span>
                <span className="palanquee-dropzone-label">Non assignés</span>
                <span className="palanquee-dropzone-count">{unassigned.length}</span>
              </div>
              <div className="palanquee-cards-area palanquee-cards-area--pool">
                {unassigned.map(d => (
                  <DiverCard key={d.id} diver={d} onDragStart={() => {}} onDragEnter={() => {}} isDragging={false}
                    onLevelChange={handleLevelChange} onAptitudesChange={handleAptitudesChange}
                    onTap={id => handleMobilePick(id, null)} isPicked={mobilePickedId === d.id}
                    aptitudesOptions={aptitudesOptions} isReadOnly={isOverviewReadOnly} />
                ))}
                {unassigned.length === 0 && <div className="palanquee-empty-hint">Tous les plongeurs sont assignés</div>}
              </div>
            </div>
          ) : (
            <DropZone palanqueeId={null} label="Non assignés" labelIcon="🏊" divers={unassigned}
              draggedId={draggedId} onDragStart={handleDragStart} onDragEnterCard={handleDragEnterCard}
              onDragEnterEnd={handleDragEnterEnd} onDrop={handleDrop}
              onLevelChange={handleLevelChange} onAptitudesChange={handleAptitudesChange}
              insertBeforeId={insertTarget?.palId === null ? insertTarget.beforeId : undefined}
              isUnassigned isPool aptitudesOptions={aptitudesOptions} isReadOnly={isOverviewReadOnly} />
          )}
        </div>

        {/* Vue mobile : navigation par points + palanquée active */}
        {isMobile && (
          <div className="palanquee-mobile-view">
            {filteredPals.length === 0 ? (
              <div className="palanquee-empty-state">
                <p>Aucune palanquée créée.</p>
                {!isOverviewReadOnly && (
                  <button className="palanquee-add-btn" onClick={handleAddPalanquee} disabled={saving}>
                    + Créer la première palanquée
                  </button>
                )}
              </div>
            ) : (
              <>
                <div className="palanquee-mobile-nav">
                  <button className="palanquee-mobile-nav-btn" disabled={activePalIdx === 0} onClick={() => setActivePalIdx(i => Math.max(0, i - 1))}>‹</button>
                  <div className="palanquee-mobile-nav-dots">
                    {filteredPals.map((_, i) => (
                      <button key={i} className={`palanquee-mobile-nav-dot${i === activePalIdx ? ' palanquee-mobile-nav-dot--active' : ''}`} onClick={() => setActivePalIdx(i)} />
                    ))}
                  </div>
                  <button className="palanquee-mobile-nav-btn" disabled={activePalIdx >= filteredPals.length - 1} onClick={() => setActivePalIdx(i => Math.min(filteredPals.length - 1, i + 1))}>›</button>
                </div>
                {filteredPals[activePalIdx] && (() => {
                  const pal = filteredPals[activePalIdx];
                  const palGlobal = filteredPals.findIndex(p => p.id === pal.id);
                  return (
                    <div className="palanquee-column palanquee-column--mobile">
                      <div className="palanquee-column-header">
                        {renamingId === pal.id ? (
                          <input ref={renameInputRef} className="palanquee-rename-input"
                            value={renameDraft} onChange={e => setRenameDraft(e.target.value)}
                            onBlur={() => commitRename(pal.id)}
                            onKeyDown={e => { if (e.key === 'Enter') commitRename(pal.id); if (e.key === 'Escape') setRenamingId(null); }} />
                        ) : (
                          <div className="palanquee-column-header-top">
                            <span className="palanquee-column-name" onDoubleClick={() => { setRenamingId(pal.id); setRenameDraft(pal.name); }}>
                              P{palGlobal + 1} — {pal.name}
                            </span>
                            <button className="palanquee-delete-btn" onClick={() => handleDeletePalanquee(pal.id)} title="Supprimer la palanquée">✕</button>
                          </div>
                        )}
                        <div className="palanquee-column-params">
                          <select
                            className={`palanquee-param-select${!pal.depth ? ' palanquee-param-select--empty' : ''}`}
                            value={pal.depth ?? ''}
                            onChange={e => handleDepthChange(pal.id, e.target.value)}
                            title="Profondeur"
                          >
                            <option value="">Prof. ▾</option>
                            {DEPTH_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                          </select>
                          <select
                            className={`palanquee-param-select${!pal.duration ? ' palanquee-param-select--empty' : ''}`}
                            value={pal.duration ?? ''}
                            onChange={e => handleDurationChange(pal.id, e.target.value)}
                            title="Durée"
                          >
                            <option value="">Temps ▾</option>
                            {DURATION_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                          </select>
                        </div>
                      </div>
                      <div className="palanquee-dropzone">
                        <div className="palanquee-cards-area">
                          {pal.divers.map(d => (
                            <DiverCard key={d.id} diver={d} onDragStart={() => {}} onDragEnter={() => {}} isDragging={false}
                              onLevelChange={handleLevelChange} onAptitudesChange={handleAptitudesChange}
                              onTap={isOverviewReadOnly ? undefined : id => handleMobilePick(id, pal.id)} isPicked={mobilePickedId === d.id}
                              aptitudesOptions={aptitudesOptions} isReadOnly={isOverviewReadOnly} />
                          ))}
                          {pal.divers.length === 0 && <div className="palanquee-empty-hint">Appuyez sur un plongeur du pool pour l'assigner ici</div>}
                        </div>
                      </div>
                    </div>
                  );
                })()}
              </>
            )}
          </div>
        )}

        {/* Vue desktop : plateau complet avec toutes les colonnes */}
        {!isMobile && (
          <div className="palanquee-board" ref={boardRef} onDragEnd={() => { setDraggedId(null); setInsertTarget(null); }}>
            {filteredPals.length === 0 ? (
              <div className="palanquee-column palanquee-column--empty">
                <div className="palanquee-empty-state">
                  <p>Aucune palanquée créée.</p>
                  {!isOverviewReadOnly && (
                    <button className="palanquee-add-btn" onClick={handleAddPalanquee} disabled={saving}>
                      + Créer la première palanquée
                    </button>
                  )}
                </div>
              </div>
            ) : (
              filteredPals.map((pal, palGlobal) => (
                <div key={pal.id} className="palanquee-column">
                  <div className="palanquee-column-header">
                    {renamingId === pal.id ? (
                      <input ref={renameInputRef} className="palanquee-rename-input"
                        value={renameDraft} onChange={e => setRenameDraft(e.target.value)}
                        onBlur={() => commitRename(pal.id)}
                        onKeyDown={e => { if (e.key === 'Enter') commitRename(pal.id); if (e.key === 'Escape') setRenamingId(null); }} />
                    ) : (
                      <div className="palanquee-column-header-top">
                        <span className="palanquee-column-name" onDoubleClick={() => { setRenamingId(pal.id); setRenameDraft(pal.name); }}>
                          P{palGlobal + 1} — {pal.name}
                        </span>
                        <button className="palanquee-delete-btn" onClick={() => handleDeletePalanquee(pal.id)} title="Supprimer la palanquée">✕</button>
                      </div>
                    )}
                    <div className="palanquee-column-params">
                      <select
                        className={`palanquee-param-select${!pal.depth ? ' palanquee-param-select--empty' : ''}`}
                        value={pal.depth ?? ''}
                        onChange={e => handleDepthChange(pal.id, e.target.value)}
                        title="Profondeur"
                      >
                        <option value="">Prof. ▾</option>
                        {DEPTH_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                      </select>
                      <select
                        className={`palanquee-param-select${!pal.duration ? ' palanquee-param-select--empty' : ''}`}
                        value={pal.duration ?? ''}
                        onChange={e => handleDurationChange(pal.id, e.target.value)}
                        title="Durée"
                      >
                        <option value="">Temps ▾</option>
                        {DURATION_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                      </select>
                    </div>
                  </div>
                  <DropZone palanqueeId={pal.id} label={pal.name} divers={pal.divers}
                    draggedId={draggedId} onDragStart={handleDragStart} onDragEnterCard={handleDragEnterCard}
                    onDragEnterEnd={handleDragEnterEnd} onDrop={handleDrop}
                    onLevelChange={handleLevelChange} onAptitudesChange={handleAptitudesChange}
                    insertBeforeId={insertTarget?.palId === pal.id ? insertTarget.beforeId : undefined}
                    palanqueeIndex={palGlobal + 1}
                    aptitudesOptions={aptitudesOptions} isReadOnly={isOverviewReadOnly} />
                </div>
              ))
            )}
          </div>
        )}
      </div>

      {/* Barre d'action mobile — apparaît quand un plongeur est sélectionné */}
      {isMobile && mobilePickedId !== null && (() => {
        const picked = allDivers.find(d => d.id === mobilePickedId);
        if (!picked) return null;
        return (
          <div className="palanquee-mobile-action-bar">
            <div className="palanquee-mobile-action-info">
              <span className="palanquee-mobile-action-name">{picked.firstName} {picked.lastName}</span>
              <span className="palanquee-mobile-action-level" style={{ color: getLevelColor(picked.level) }}>{picked.level}</span>
            </div>
            <div className="palanquee-mobile-action-btns">
              <button
                className={`palanquee-mobile-action-btn palanquee-mobile-action-btn--pool${mobileFromPalId === null ? ' palanquee-mobile-action-btn--active' : ''}`}
                onClick={() => handleMobileAssign(null)}
              >📋 Non assignés</button>
              {filteredPals.map((pal, i) => (
                <button
                  key={pal.id}
                  className={`palanquee-mobile-action-btn${mobileFromPalId === pal.id ? ' palanquee-mobile-action-btn--active' : ''}`}
                  onClick={() => { handleMobileAssign(pal.id); setActivePalIdx(i); }}
                >P{i + 1} {pal.name}</button>
              ))}
              <button
                className="palanquee-mobile-action-btn palanquee-mobile-action-btn--cancel"
                onClick={() => { setMobilePickedId(null); setMobileFromPalId(null); }}
              >✕ Annuler</button>
            </div>
          </div>
        );
      })()}

    </div>
  );
}
