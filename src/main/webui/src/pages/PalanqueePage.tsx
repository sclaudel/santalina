import { useState, useEffect, useRef, useCallback } from 'react';
import { slotDiverService } from '../services/slotDiverService';
import { palanqueeService } from '../services/palanqueeService';
import { slotService } from '../services/slotService';
import type { DiveSlot, SlotDiver, Palanquee } from '../types';

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

function getLevelColor(level: string) {
  return LEVEL_COLORS[level] ?? '#6b7280';
}

function fmtDate(d: string) {
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

// ── DiverCard ────────────────────────────────────────────────────────────────
interface DiverCardProps {
  diver: SlotDiver;
  onDragStart: (id: number) => void;
  onDragEnter: () => void;
  isDragging: boolean;
  onLevelChange: (diverId: number, newLevel: string) => void;
  onAptitudesChange: (diverId: number, newAptitudes: string) => void;
  onTap?: (id: number) => void;
  isPicked?: boolean;
}

const APTITUDES_OPTIONS = ['PE12','PE20','PE40','PE60','PA12','PA20','PA40','PA60','E1','E2','E3','E4','GP'];
const DEPTH_OPTIONS = ['6m', '12m', '20m', '30m', '40m', '50m', '60m'];
const DURATION_OPTIONS = Array.from({ length: 24 }, (_, i) => `${(i + 1) * 10}'`);

function DiverCard({ diver, onDragStart, onDragEnter, isDragging, onLevelChange, onAptitudesChange, onTap, isPicked }: DiverCardProps) {
  const [editingLevel, setEditingLevel] = useState(false);
  const [editingAptitudes, setEditingAptitudes] = useState(false);
  const color = getLevelColor(diver.level);

  const levelOptions = Object.keys(LEVEL_COLORS);
  if (diver.level && !levelOptions.includes(diver.level)) levelOptions.unshift(diver.level);

  const isEditing = editingLevel || editingAptitudes;

  return (
    <div
      className={`palanquee-postit${isDragging ? ' palanquee-postit--dragging' : ''}${isPicked ? ' palanquee-postit--picked' : ''}`}
      draggable={!isEditing && !onTap}
      onDragStart={e => {
        if (isEditing) { e.preventDefault(); return; }
        e.dataTransfer.effectAllowed = 'move';
        onDragStart(diver.id);
      }}
      onDragEnter={e => { e.stopPropagation(); if (!isEditing) onDragEnter(); }}
      onClick={() => onTap?.(diver.id)}
      style={{ borderTop: `4px solid ${color}` }}
      title={onTap ? 'Appuyer pour sélectionner' : 'Glisser pour réordonner ou changer de palanquée'}
    >
      <div className="palanquee-postit-name">
        {diver.isDirector && <span className="palanquee-postit-director" title="Directeur de plongée">🎖 </span>}
        {diver.firstName} {diver.lastName}
      </div>
      {editingLevel ? (
        <select
          autoFocus
          className="palanquee-postit-level-select"
          value={diver.level}
          onBlur={() => setEditingLevel(false)}
          onChange={e => { onLevelChange(diver.id, e.target.value); setEditingLevel(false); }}
          onMouseDown={e => e.stopPropagation()}
          onClick={e => e.stopPropagation()}
        >
          {levelOptions.map(l => <option key={l} value={l}>{l}</option>)}
        </select>
      ) : (
        <div
          className="palanquee-postit-level"
          style={{ color }}
          title="Double-clic pour modifier le niveau"
          onDoubleClick={e => { e.stopPropagation(); setEditingLevel(true); }}
        >
          {diver.level}
        </div>
      )}
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
          {APTITUDES_OPTIONS.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
      ) : (
        <div
          className="palanquee-postit-aptitudes"
          title="Double-clic pour modifier les aptitudes"
          onDoubleClick={e => { e.stopPropagation(); setEditingAptitudes(true); }}
        >
          {diver.aptitudes ? diver.aptitudes : <span className="palanquee-postit-aptitudes--empty">aptitudes</span>}
        </div>
      )}
    </div>
  );
}

// ── DropZone ─────────────────────────────────────────────────────────────────
interface DropZoneProps {
  palanqueeId: number | null;
  divers: SlotDiver[];
  draggedId: number | null;
  onDrop: (palanqueeId: number | null) => void;
  onDragStart: (id: number) => void;
  onDragEnterCard: (palanqueeId: number | null, beforeDiverId: number) => void;
  onDragEnterEnd: (palanqueeId: number | null) => void;
  insertBeforeId?: number | null; // undefined = inactive, null = at end, number = before that id
  label: string;
  labelIcon: string;
  isUnassigned?: boolean;
  isPool?: boolean;         // layout horizontal wrap (pool non-assignés)
  palanqueeIndex?: number;  // 1-based number for display
  onLevelChange: (diverId: number, newLevel: string) => void;
  onAptitudesChange: (diverId: number, newAptitudes: string) => void;
  onTapDiver?: (id: number) => void;   // mobile: tap to pick
  mobilePickedId?: number | null;      // mobile: highlight picked diver
}

function DropZone({
  palanqueeId, divers, draggedId, onDrop, onDragStart,
  onDragEnterCard, onDragEnterEnd, insertBeforeId,
  label, labelIcon, isUnassigned = false, isPool = false, palanqueeIndex,
  onLevelChange, onAptitudesChange, onTapDiver, mobilePickedId,
}: DropZoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setIsDragOver(true);
  };
  const handleDragLeave = (e: React.DragEvent) => {
    if (!e.currentTarget.contains(e.relatedTarget as Node)) setIsDragOver(false);
  };
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    onDrop(palanqueeId);
  };

  const isActive = insertBeforeId !== undefined;

  return (
    <div
      className={`palanquee-dropzone${isDragOver ? ' palanquee-dropzone--over' : ''}${isUnassigned ? ' palanquee-dropzone--unassigned' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="palanquee-dropzone-header">
        <span className="palanquee-dropzone-icon">{labelIcon}</span>
        <span className="palanquee-dropzone-label">
          {palanqueeIndex !== undefined ? `P${palanqueeIndex} – ` : ''}{label}
        </span>
        <span className="palanquee-dropzone-count">{divers.length}</span>
      </div>
      <div
        className={`palanquee-cards-area${isPool ? ' palanquee-cards-area--pool' : ''}`}
        onDragEnter={() => onDragEnterEnd(palanqueeId)}
      >
        {divers.map(d => (
          <>
            {isActive && insertBeforeId === d.id && (
              <div key={`ins-${d.id}`} className="palanquee-insert-line" />
            )}
            <DiverCard
              key={d.id}
              diver={d}
              onDragStart={onDragStart}
              onDragEnter={() => onDragEnterCard(palanqueeId, d.id)}
              isDragging={d.id === draggedId}
              onLevelChange={onLevelChange}
              onAptitudesChange={onAptitudesChange}
              onTap={onTapDiver}
              isPicked={mobilePickedId === d.id}
            />
          </>
        ))}
        {isActive && insertBeforeId === null && (
          <div className="palanquee-insert-line" />
        )}
        {divers.length === 0 && (
          <div className="palanquee-empty-hint">Déposer un plongeur ici</div>
        )}
      </div>
    </div>
  );
}

// ── PalanqueePage ─────────────────────────────────────────────────────────────
interface Props {
  slotId: number;
  onBack: () => void;
}

export function PalanqueePage({ slotId, onBack }: Props) {
  const [slot, setSlot]             = useState<DiveSlot | null>(null);
  const [allDivers, setAllDivers]   = useState<SlotDiver[]>([]);
  const [palanquees, setPalanquees] = useState<Palanquee[]>([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [draggedId, setDraggedId]   = useState<number | null>(null);

  // renaming state: palanqueeId → draft name
  const [renamingId, setRenamingId]     = useState<number | null>(null);
  const [renameDraft, setRenameDraft]   = useState('');
  const renameInputRef = useRef<HTMLInputElement>(null);

  const [saving, setSaving] = useState(false);
  const [exporting, setExporting] = useState(false);

  // ── mobile ─────────────────────────────────────────────────────────────────
  const [isMobile, setIsMobile] = useState(() => window.matchMedia('(max-width: 768px)').matches);
  const [mobilePickedId, setMobilePickedId] = useState<number | null>(null);
  const [activePalIdx, setActivePalIdx] = useState(0);

  // référence sur le board pour l'auto-scroll horizontal pendant le drag
  const boardRef = useRef<HTMLDivElement>(null);

  // insertTarget: quel plongeur on va insérer AVANT (null = fin de liste)
  const [insertTarget, setInsertTarget] = useState<{
    palanqueeId: number | null;
    beforeDiverId: number | null;
  } | null>(null);
  const insertTargetRef = useRef(insertTarget);
  insertTargetRef.current = insertTarget;

  // ── chargement ────────────────────────────────────────────────────────────
  const loadAll = useCallback(async () => {
    setError('');
    try {
      const [slotData, diversData, pData] = await Promise.all([
        slotService.getById(slotId),
        slotDiverService.getBySlot(slotId),
        palanqueeService.getBySlot(slotId),
      ]);
      setSlot(slotData);
      setAllDivers(diversData);
      setPalanquees(pData);
    } catch {
      setError('Impossible de charger les données du créneau.');
    } finally {
      setLoading(false);
    }
  }, [slotId]);

  useEffect(() => { loadAll(); }, [loadAll]);

  // Suivi responsive
  useEffect(() => {
    const mq = window.matchMedia('(max-width: 768px)');
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  // Maintenir activePalIdx dans les bornes
  useEffect(() => {
    if (palanquees.length > 0 && activePalIdx >= palanquees.length) {
      setActivePalIdx(palanquees.length - 1);
    }
  }, [palanquees.length, activePalIdx]);

  // ── changement de niveau inline sur le post-it ─────────────────────────────
  const handleLevelChange = useCallback(async (diverId: number, newLevel: string) => {
    const diver = allDivers.find(d => d.id === diverId);
    if (!diver || diver.level === newLevel) return;
    const updater = (d: SlotDiver) => d.id === diverId ? { ...d, level: newLevel } : d;
    setAllDivers(prev => prev.map(updater));
    setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.map(updater) })));
    try {
      await slotDiverService.update(slotId, diverId, {
        firstName:  diver.firstName,
        lastName:   diver.lastName,
        level:      newLevel,
        email:      diver.email,
        phone:      diver.phone,
        isDirector: diver.isDirector,
        aptitudes:  diver.aptitudes,
        licenseNumber: diver.licenseNumber,
      });
    } catch {
      await loadAll();
    }
  }, [allDivers, slotId, loadAll]);

  // ── changement d'aptitudes inline sur le post-it ───────────────────────────
  const handleAptitudesChange = useCallback(async (diverId: number, newAptitudes: string) => {
    const diver = allDivers.find(d => d.id === diverId);
    if (!diver || diver.aptitudes === newAptitudes) return;
    const updater = (d: SlotDiver) => d.id === diverId ? { ...d, aptitudes: newAptitudes || undefined } : d;
    setAllDivers(prev => prev.map(updater));
    setPalanquees(prev => prev.map(p => ({ ...p, divers: p.divers.map(updater) })));
    try {
      await slotDiverService.update(slotId, diverId, {
        firstName:  diver.firstName,
        lastName:   diver.lastName,
        level:      diver.level,
        email:      diver.email,
        phone:      diver.phone,
        isDirector: diver.isDirector,
        aptitudes:  newAptitudes || undefined,
        licenseNumber: diver.licenseNumber,
      });
    } catch {
      await loadAll();
    }
  }, [allDivers, slotId, loadAll]);

  // Auto-scroll horizontal du board lors du drag vers les bords
  useEffect(() => {
    const board = boardRef.current;
    if (!board) return;

    const EDGE = 80;       // px à partir du bord qui déclenche le scroll
    const MAX_SPEED = 16;  // px par frame
    let dir = 0;
    let speed = 0;
    let frame: number | null = null;

    const tick = () => {
      if (dir !== 0) {
        board.scrollLeft += speed * dir;
        frame = requestAnimationFrame(tick);
      } else {
        frame = null;
      }
    };

    const onDragOver = (e: DragEvent) => {
      const rect = board.getBoundingClientRect();
      const x = e.clientX - rect.left;
      if (x < EDGE) {
        dir = -1;
        speed = Math.round(MAX_SPEED * (1 - x / EDGE));
      } else if (x > rect.width - EDGE) {
        dir = 1;
        speed = Math.round(MAX_SPEED * (1 - (rect.width - x) / EDGE));
      } else {
        dir = 0;
      }
      if (dir !== 0 && frame === null) frame = requestAnimationFrame(tick);
    };

    const stop = () => {
      dir = 0;
      if (frame !== null) { cancelAnimationFrame(frame); frame = null; }
    };

    board.addEventListener('dragover', onDragOver);
    board.addEventListener('dragleave', stop);
    board.addEventListener('drop', stop);
    board.addEventListener('dragend', stop);
    return () => {
      board.removeEventListener('dragover', onDragOver);
      board.removeEventListener('dragleave', stop);
      board.removeEventListener('drop', stop);
      board.removeEventListener('dragend', stop);
      stop();
    };
  }, []);

  // Focus automatique sur le champ de renommage
  useEffect(() => {
    if (renamingId !== null) renameInputRef.current?.focus();
  }, [renamingId]);

  // ── calcul des non-assignés ───────────────────────────────────────────────
  const assignedIds = new Set(palanquees.flatMap(p => p.divers.map(d => d.id)));
  const unassigned  = allDivers.filter(d => !assignedIds.has(d.id));

  // ── assignation commune (DnD + tap mobile) ───────────────────────────────
  const handleAssign = useCallback(async (
    diverId: number,
    targetPalanqueeId: number | null,
    beforeDiverId: number | null = null,
  ) => {
    const currentPalanquee = palanquees.find(p => p.divers.some(d => d.id === diverId));
    const currentPalanqueeId = currentPalanquee?.id ?? null;
    const diver = allDivers.find(d => d.id === diverId);
    if (!diver) return;

    if (currentPalanqueeId === targetPalanqueeId && targetPalanqueeId !== null) {
      // ── Réordonner au sein de la même palanquée ────────────────────────
      const pal = palanquees.find(p => p.id === targetPalanqueeId)!;
      const without = pal.divers.filter(d => d.id !== diverId);
      let reordered: SlotDiver[];
      if (beforeDiverId === null) {
        reordered = [...without, diver];
      } else {
        const idx = without.findIndex(d => d.id === beforeDiverId);
        reordered = idx === -1
          ? [...without, diver]
          : [...without.slice(0, idx), diver, ...without.slice(idx)];
      }
      setPalanquees(prev => prev.map(p => p.id === targetPalanqueeId ? { ...p, divers: reordered } : p));
      try {
        await palanqueeService.reorder(slotId, targetPalanqueeId, reordered.map(d => d.id));
      } catch { await loadAll(); }
      return;
    }

    // ── Déplacer vers une autre palanquée (ou zone non assignée) ──────────
    setPalanquees(prev => prev.map(p => {
      if (p.id === currentPalanqueeId) return { ...p, divers: p.divers.filter(d => d.id !== diverId) };
      if (p.id === targetPalanqueeId) {
        const without = p.divers.filter(d => d.id !== diverId);
        if (beforeDiverId === null) return { ...p, divers: [...without, diver] };
        const idx = without.findIndex(d => d.id === beforeDiverId);
        return { ...p, divers: idx === -1 ? [...without, diver] : [...without.slice(0, idx), diver, ...without.slice(idx)] };
      }
      return p;
    }));

    try {
      await palanqueeService.assign(slotId, diverId, targetPalanqueeId);
      if (targetPalanqueeId !== null) {
        const updatedPal = palanquees.find(p => p.id === targetPalanqueeId)!;
        const without = updatedPal.divers.filter(d => d.id !== diverId);
        const finalOrder: SlotDiver[] = beforeDiverId === null
          ? [...without, diver]
          : (() => {
              const idx = without.findIndex(d => d.id === beforeDiverId);
              return idx === -1 ? [...without, diver] : [...without.slice(0, idx), diver, ...without.slice(idx)];
            })();
        await palanqueeService.reorder(slotId, targetPalanqueeId, finalOrder.map(d => d.id));
      }
    } catch { await loadAll(); }
  }, [allDivers, palanquees, slotId, loadAll]);

  // ── drag & drop ───────────────────────────────────────────────────────────
  const handleDragStart = (diverId: number) => {
    setDraggedId(diverId);
    setInsertTarget(null);
  };

  const handleDragEnterCard = (palanqueeId: number | null, beforeDiverId: number) => {
    setInsertTarget({ palanqueeId, beforeDiverId });
  };

  const handleDragEnterEnd = (palanqueeId: number | null) => {
    setInsertTarget({ palanqueeId, beforeDiverId: null });
  };

  const handleDrop = async (targetPalanqueeId: number | null) => {
    if (draggedId === null) return;
    const target = insertTargetRef.current;
    const did = draggedId;
    setDraggedId(null);
    setInsertTarget(null);
    const beforeId = target?.palanqueeId === targetPalanqueeId ? target.beforeDiverId : null;
    await handleAssign(did, targetPalanqueeId, beforeId);
  };

  // ── tap mobile ────────────────────────────────────────────────────────────
  const handleMobilePick = (diverId: number) => {
    setMobilePickedId(prev => prev === diverId ? null : diverId);
  };

  const handleMobileAssign = async (targetPalanqueeId: number | null) => {
    if (mobilePickedId === null) return;
    const did = mobilePickedId;
    setMobilePickedId(null);
    await handleAssign(did, targetPalanqueeId);
  };

  // ── actions ───────────────────────────────────────────────────────────────
  const handleAddPalanquee = async () => {
    setSaving(true);
    try {
      const newName = `Palanquée ${palanquees.length + 1}`;
      const created = await palanqueeService.create(slotId, newName);
      setPalanquees(prev => [...prev, created]);
    } catch {
      setError('Impossible de créer la palanquée.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePalanquee = async (id: number) => {
    const p = palanquees.find(x => x.id === id);
    if (!p) return;
    if (p.divers.length > 0) {
      if (!window.confirm(`Supprimer « ${p.name} » et désassigner ses ${p.divers.length} plongeur(s) ?`)) return;
    }
    try {
      await palanqueeService.delete(slotId, id);
      setPalanquees(prev => prev.filter(x => x.id !== id));
    } catch {
      setError('Impossible de supprimer la palanquée.');
    }
  };

  const startRename = (p: Palanquee) => {
    setRenamingId(p.id);
    setRenameDraft(p.name);
  };

  const commitRename = async (id: number) => {
    const draft = renameDraft.trim();
    if (!draft) { setRenamingId(null); return; }
    const pal  = palanquees.find(p => p.id === id);
    const prev = pal?.name;
    if (draft === prev) { setRenamingId(null); return; }
    setPalanquees(ps => ps.map(p => p.id === id ? { ...p, name: draft } : p));
    setRenamingId(null);
    try {
      await palanqueeService.rename(slotId, id, draft, pal?.depth, pal?.duration);
    } catch {
      await loadAll();
    }
  };

  const handleDepthChange = useCallback(async (palanqueeId: number, depth: string) => {
    const pal = palanquees.find(p => p.id === palanqueeId);
    if (!pal) return;
    setPalanquees(prev => prev.map(p => p.id === palanqueeId ? { ...p, depth: depth || undefined } : p));
    try {
      await palanqueeService.rename(slotId, palanqueeId, pal.name, depth || undefined, pal.duration);
    } catch { await loadAll(); }
  }, [palanquees, slotId, loadAll]);

  const handleDurationChange = useCallback(async (palanqueeId: number, duration: string) => {
    const pal = palanquees.find(p => p.id === palanqueeId);
    if (!pal) return;
    setPalanquees(prev => prev.map(p => p.id === palanqueeId ? { ...p, duration: duration || undefined } : p));
    try {
      await palanqueeService.rename(slotId, palanqueeId, pal.name, pal.depth, duration || undefined);
    } catch { await loadAll(); }
  }, [palanquees, slotId, loadAll]);

  // ── export Excel ─────────────────────────────────────────────────────────
  const handleExportExcel = async () => {
    if (!slot) return;
    setExporting(true);
    try {
      const { exportFicheSecuriteAvecPalanquees } = await import('../utils/exportFicheSecuriteAvecPalanquees');
      await exportFicheSecuriteAvecPalanquees(slot, allDivers, palanquees);
    } catch (err) {
      console.error('Export palanquées :', err);
    } finally {
      setExporting(false);
    }
  };

  // ── rendu ─────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="palanquee-page">
        <div className="palanquee-loading">Chargement…</div>
      </div>
    );
  }

  return (
    <div className="palanquee-page">
      {/* En-tête */}
      <div className="palanquee-page-header">
        <button className="palanquee-back-btn" onClick={onBack}>← Retour</button>
        <div className="palanquee-page-title-block">
          <h2 className="palanquee-page-title">🤿 Organisation des palanquées</h2>
          {slot && (
            <div className="palanquee-page-subtitle">
              <span>{fmtDate(slot.slotDate)}</span>
              <span className="palanquee-separator">·</span>
              <span>{slot.startTime}–{slot.endTime}</span>
              {slot.title && <><span className="palanquee-separator">·</span><span>{slot.title}</span></>}
              {slot.club  && <><span className="palanquee-separator">·</span><span>🏊 {slot.club}</span></>}
              <span className="palanquee-separator">·</span>
              <span>🤿 {allDivers.length}/{slot.diverCount}</span>
            </div>
          )}
        </div>
        <div className="palanquee-page-actions">
          <button
            className="palanquee-add-btn"
            onClick={handleAddPalanquee}
            disabled={saving}
          >
            + Nouvelle palanquée
          </button>
          <button
            className="palanquee-export-btn"
            onClick={handleExportExcel}
            disabled={exporting || allDivers.length === 0}
            title="Exporter la fiche de sécurité Excel avec les palanquées"
          >
            {exporting ? '…' : '📥 Export Excel'}
          </button>
        </div>
      </div>

      {error && <div className="palanquee-error">{error}</div>}

      <div className="palanquee-hint">
        {isMobile
          ? <span>💡 Appuyez sur un plongeur pour le sélectionner, puis choisissez sa destination</span>
          : <span>💡 Glissez les post-its pour assigner ou réordonner les plongeurs</span>
        }
        {unassigned.length > 0 && (
          <span className="palanquee-hint-warning">  ⚠️ {unassigned.length} plongeur{unassigned.length > 1 ? 's' : ''} non assigné{unassigned.length > 1 ? 's' : ''}</span>
        )}
      </div>

      {/* Pool non-assignés — sticky sur mobile, fixe au-dessus du board */}
      <div className={`palanquee-pool-section${isMobile ? ' palanquee-pool-section--sticky' : ''}`}>
        <DropZone
          palanqueeId={null}
          divers={unassigned}
          draggedId={draggedId}
          onDrop={handleDrop}
          onDragStart={handleDragStart}
          onDragEnterCard={handleDragEnterCard}
          onDragEnterEnd={handleDragEnterEnd}
          label="Non assignés"
          labelIcon="📋"
          isUnassigned
          isPool
          onLevelChange={handleLevelChange}
          onAptitudesChange={handleAptitudesChange}
          onTapDiver={isMobile ? handleMobilePick : undefined}
          mobilePickedId={isMobile ? mobilePickedId : undefined}
        />
      </div>

      {/* ── Mobile : navigation + palanquée active ── */}
      {isMobile && (
        <div className="palanquee-mobile-view">
          {palanquees.length === 0 ? (
            <div className="palanquee-empty-state">
              <p>Aucune palanquée créée.</p>
              <button className="palanquee-add-btn" onClick={handleAddPalanquee} disabled={saving}>
                + Créer la première palanquée
              </button>
            </div>
          ) : (
            <>
              {/* Navigation palanquées */}
              <div className="palanquee-mobile-nav">
                <button
                  className="palanquee-mobile-nav-btn"
                  onClick={() => setActivePalIdx(i => Math.max(0, i - 1))}
                  disabled={activePalIdx === 0}
                >‹</button>
                <div className="palanquee-mobile-nav-dots">
                  {palanquees.map((_, i) => (
                    <button
                      key={i}
                      className={`palanquee-mobile-nav-dot${i === activePalIdx ? ' palanquee-mobile-nav-dot--active' : ''}`}
                      onClick={() => setActivePalIdx(i)}
                    />
                  ))}
                </div>
                <button
                  className="palanquee-mobile-nav-btn"
                  onClick={() => setActivePalIdx(i => Math.min(palanquees.length - 1, i + 1))}
                  disabled={activePalIdx >= palanquees.length - 1}
                >›</button>
              </div>

              {/* Palanquée active */}
              {palanquees[activePalIdx] && (() => {
                const p = palanquees[activePalIdx];
                const idx = activePalIdx;
                return (
                  <div className="palanquee-column palanquee-column--mobile">
                    <div className="palanquee-column-header">
                      {renamingId === p.id ? (
                        <input
                          ref={renameInputRef}
                          className="palanquee-rename-input"
                          value={renameDraft}
                          onChange={e => setRenameDraft(e.target.value)}
                          onBlur={() => commitRename(p.id)}
                          onKeyDown={e => {
                            if (e.key === 'Enter') commitRename(p.id);
                            if (e.key === 'Escape') setRenamingId(null);
                          }}
                        />
                      ) : (
                        <span
                          className="palanquee-column-name"
                          title="Appui long pour renommer"
                          onDoubleClick={() => startRename(p)}
                        >
                          P{idx + 1} – {p.name}
                        </span>
                      )}
                      <div className="palanquee-column-params">
                        <select
                          className={`palanquee-param-select${!p.depth ? ' palanquee-param-select--empty' : ''}`}
                          value={p.depth ?? ''}
                          onChange={e => handleDepthChange(p.id, e.target.value)}
                          title="Cliquez pour définir la profondeur maximale de cette palanquée"
                        >
                          <option value="">Prof. ▾</option>
                          {DEPTH_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                        </select>
                        <select
                          className={`palanquee-param-select${!p.duration ? ' palanquee-param-select--empty' : ''}`}
                          value={p.duration ?? ''}
                          onChange={e => handleDurationChange(p.id, e.target.value)}
                          title="Cliquez pour définir le temps maximum de cette palanquée"
                        >
                          <option value="">Temps ▾</option>
                          {DURATION_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                        </select>
                      </div>
                      <button
                        className="palanquee-delete-btn"
                        onClick={() => handleDeletePalanquee(p.id)}
                        title="Supprimer cette palanquée"
                      >✕</button>
                    </div>
                    <DropZone
                      palanqueeId={p.id}
                      divers={p.divers}
                      draggedId={draggedId}
                      onDrop={handleDrop}
                      onDragStart={handleDragStart}
                      onDragEnterCard={handleDragEnterCard}
                      onDragEnterEnd={handleDragEnterEnd}
                      label={p.name}
                      labelIcon="🤿"
                      palanqueeIndex={idx + 1}
                      onLevelChange={handleLevelChange}
                      onAptitudesChange={handleAptitudesChange}
                      onTapDiver={handleMobilePick}
                      mobilePickedId={mobilePickedId}
                    />
                  </div>
                );
              })()}
            </>
          )}
        </div>
      )}

      {/* ── Desktop : plateau scroll horizontal ── */}
      {!isMobile && (
        <div
          ref={boardRef}
          className="palanquee-board"
          onDragEnd={() => { setDraggedId(null); setInsertTarget(null); }}
        >
          {palanquees.map((p, idx) => (
            <div key={p.id} className="palanquee-column">
              <div className="palanquee-column-header">
                {renamingId === p.id ? (
                  <input
                    ref={renameInputRef}
                    className="palanquee-rename-input"
                    value={renameDraft}
                    onChange={e => setRenameDraft(e.target.value)}
                    onBlur={() => commitRename(p.id)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') commitRename(p.id);
                      if (e.key === 'Escape') setRenamingId(null);
                    }}
                  />
                ) : (
                  <span
                    className="palanquee-column-name"
                    title="Double-clic pour renommer"
                    onDoubleClick={() => startRename(p)}
                  >
                    P{idx + 1} – {p.name}
                  </span>
                )}
                <div className="palanquee-column-params">
                  <select
                    className={`palanquee-param-select${!p.depth ? ' palanquee-param-select--empty' : ''}`}
                    value={p.depth ?? ''}
                    onChange={e => handleDepthChange(p.id, e.target.value)}
                    title="Cliquez pour définir la profondeur maximale de cette palanquée"
                  >
                    <option value="">Prof. ▾</option>
                    {DEPTH_OPTIONS.map(d => <option key={d} value={d}>{d}</option>)}
                  </select>
                  <select
                    className={`palanquee-param-select${!p.duration ? ' palanquee-param-select--empty' : ''}`}
                    value={p.duration ?? ''}
                    onChange={e => handleDurationChange(p.id, e.target.value)}
                    title="Cliquez pour définir le temps maximum de cette palanquée"
                  >
                    <option value="">Temps ▾</option>
                    {DURATION_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <button
                  className="palanquee-delete-btn"
                  onClick={() => handleDeletePalanquee(p.id)}
                  title="Supprimer cette palanquée"
                >✕</button>
              </div>
              <DropZone
                palanqueeId={p.id}
                divers={p.divers}
                draggedId={draggedId}
                onDrop={handleDrop}
                onDragStart={handleDragStart}
                onDragEnterCard={handleDragEnterCard}
                onDragEnterEnd={handleDragEnterEnd}
                insertBeforeId={insertTarget?.palanqueeId === p.id ? insertTarget.beforeDiverId : undefined}
                label={p.name}
                labelIcon="🤿"
                palanqueeIndex={idx + 1}
                onLevelChange={handleLevelChange}
                onAptitudesChange={handleAptitudesChange}
              />
            </div>
          ))}

          {palanquees.length === 0 && (
            <div className="palanquee-column palanquee-column--empty">
              <div className="palanquee-empty-state">
                <p>Aucune palanquée créée.</p>
                <button className="palanquee-add-btn" onClick={handleAddPalanquee} disabled={saving}>
                  + Créer la première palanquée
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Résumé bas de page */}
      {palanquees.length > 0 && (
        <div className="palanquee-summary">
          {palanquees.map((p, idx) => (
            <span key={p.id} className="palanquee-summary-chip">
              P{idx + 1} : {p.divers.length} plongeur{p.divers.length !== 1 ? 's' : ''}
            </span>
          ))}
        </div>
      )}

      {/* ── Barre d'action mobile (plongeur sélectionné) ── */}
      {isMobile && mobilePickedId !== null && (() => {
        const picked = allDivers.find(d => d.id === mobilePickedId);
        if (!picked) return null;
        const isInPool = !palanquees.some(p => p.divers.some(d => d.id === mobilePickedId));
        return (
          <div className="palanquee-mobile-action-bar">
            <div className="palanquee-mobile-action-info">
              <span className="palanquee-mobile-action-name">{picked.firstName} {picked.lastName}</span>
              <span className="palanquee-mobile-action-level" style={{ color: getLevelColor(picked.level) }}>
                {picked.level}
              </span>
            </div>
            <div className="palanquee-mobile-action-btns">
              {!isInPool && (
                <button
                  className="palanquee-mobile-action-btn palanquee-mobile-action-btn--pool"
                  onClick={() => handleMobileAssign(null)}
                >
                  📋 Pool
                </button>
              )}
              {palanquees.map((p, idx) => (
                <button
                  key={p.id}
                  className={`palanquee-mobile-action-btn${idx === activePalIdx ? ' palanquee-mobile-action-btn--active' : ''}`}
                  onClick={() => { handleMobileAssign(p.id); if (isInPool) setActivePalIdx(idx); }}
                >
                  P{idx + 1}
                </button>
              ))}
              <button
                className="palanquee-mobile-action-btn palanquee-mobile-action-btn--cancel"
                onClick={() => setMobilePickedId(null)}
              >
                ✕
              </button>
            </div>
          </div>
        );
      })()}
    </div>
  );
}
