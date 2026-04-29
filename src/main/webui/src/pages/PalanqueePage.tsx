import { useState, useEffect, useRef, useCallback } from 'react';
import { slotDiverService } from '../services/slotDiverService';
import { palanqueeService } from '../services/palanqueeService';
import { slotDiveService } from '../services/slotDiveService';
import { slotService } from '../services/slotService';
import { waitingListService } from '../services/waitingListService';
import { downloadSlotIcs } from '../utils/calendarExport';
import { adminService } from '../services/adminService';
import { authService } from '../services/authService';
import { slotMailService } from '../services/slotMailService';
import { exportDiverListCsv } from '../utils/exportDiverList';
import { RichTextEditor } from '../components/RichTextEditor';
import { DpOrganizerMailer } from '../utils/dpMailDefaults';
import type { DiveSlot, SlotDiver, Palanquee, SlotDive, WaitingListEntry, RegistrationStatus } from '../types';

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
  onMoveToWaitingList?: (diverId: number) => void;
  movingToWlId?: number | null;
  aptitudesOptions?: string[];
}

const APTITUDES_OPTIONS = ['PE12','PE20','PE40','PE60','PA12','PA20','PA40','PA60','E1','E2','E3','E4','GP'];
// ↑ liste de repli — remplacée par la config au chargement
const DEPTH_OPTIONS = ['6m', '12m', '20m', '30m', '40m', '50m', '60m'];
const DURATION_OPTIONS = Array.from({ length: 24 }, (_, i) => `${(i + 1) * 10}'`);

function DiverCard({ diver, onDragStart, onDragEnter, isDragging, onLevelChange, onAptitudesChange, onTap, isPicked, onMoveToWaitingList, movingToWlId, aptitudesOptions }: DiverCardProps) {
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
          {(aptitudesOptions ?? APTITUDES_OPTIONS).map(a => <option key={a} value={a}>{a}</option>)}
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
      {onMoveToWaitingList && !diver.isDirector && (
        <button
          className="palanquee-postit-wl-btn"
          title="Remettre ce plongeur en liste d'attente (annule sa place confirmée)"
          disabled={movingToWlId === diver.id}
          onClick={e => { e.stopPropagation(); onMoveToWaitingList(diver.id); }}
          onMouseDown={e => e.stopPropagation()}
        >
          {movingToWlId === diver.id ? (
            <span className="palanquee-postit-wl-btn__spinner">⏳</span>
          ) : (
            <>Remettre en liste d'attente</>
          )}
        </button>
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
  onMoveToWaitingList?: (diverId: number) => void;
  movingToWlId?: number | null;
  aptitudesOptions?: string[];
}

function DropZone({
  palanqueeId, divers, draggedId, onDrop, onDragStart,
  onDragEnterCard, onDragEnterEnd, insertBeforeId,
  label, labelIcon, isUnassigned = false, isPool = false, palanqueeIndex,
  onLevelChange, onAptitudesChange, onTapDiver, mobilePickedId,
  onMoveToWaitingList, movingToWlId, aptitudesOptions,
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
              onMoveToWaitingList={onMoveToWaitingList}
              movingToWlId={movingToWlId}
              aptitudesOptions={aptitudesOptions}
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

// ── helpers ──────────────────────────────────────────────────────────────────

/** Retourne true si les inscriptions sont actuellement actives sur le créneau,
 *  en reproduisant la logique backend (registrationOpen + registrationOpensAt). */
function isRegistrationCurrentlyActive(slot: DiveSlot | null): boolean {
  if (!slot || !slot.registrationOpen) return false;
  if (!slot.registrationOpensAt) return true;
  return new Date() >= new Date(slot.registrationOpensAt);
}

// ── PalanqueePage ─────────────────────────────────────────────────────────────
interface Props {
  slotId: number;
  onBack: (slotDate?: string) => void;
}

export function PalanqueePage({ slotId, onBack }: Props) {
  const [slot, setSlot]             = useState<DiveSlot | null>(null);
  const [allDivers, setAllDivers]   = useState<SlotDiver[]>([]);
  const [palanquees, setPalanquees] = useState<Palanquee[]>([]);
  const [slotDives, setSlotDives]   = useState<SlotDive[]>([]);
  // null = toutes les plongées (pas de filtre), number = ID de la plongée active
  const [activeDiveId, setActiveDiveId] = useState<number | null>(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [draggedId, setDraggedId]   = useState<number | null>(null);

  // Liste d'attente
  const [waitingList, setWaitingList]     = useState<WaitingListEntry[]>([]);
  const [approvingId, setApprovingId]     = useState<number | null>(null);
  const [cancelingId, setCancelingId]     = useState<number | null>(null);
  const [wlError, setWlError]             = useState('');
  const [movingToWlId, setMovingToWlId]   = useState<number | null>(null);

  // Mise à jour statut de vérification
  const [statusUpdatingId, setStatusUpdatingId]           = useState<number | null>(null);
  const [incompleteReasonId, setIncompleteReasonId]       = useState<number | null>(null);
  const [incompleteReason, setIncompleteReason]           = useState('');

  // renaming state: palanqueeId → draft name
  const [renamingId, setRenamingId]     = useState<number | null>(null);
  const [renameDraft, setRenameDraft]   = useState('');
  const renameInputRef = useRef<HTMLInputElement>(null);

  const [saving, setSaving] = useState(false);

  // ── Mail d'organisation DP ─────────────────────────────────────────────
  const [showMailModal, setShowMailModal] = useState(false);
  const [mailSubject, setMailSubject]     = useState('');
  const [mailBody, setMailBody]           = useState('');
  const [mailBodyKey, setMailBodyKey]     = useState(0); // force re-mount RichTextEditor
  const [emailOverrides, setEmailOverrides] = useState<Record<number, string>>({});
  const [mailAttachment, setMailAttachment] = useState<File | null>(null);
  const [mailSending, setMailSending]     = useState(false);
  const [mailSuccess, setMailSuccess]     = useState('');
  const [mailError, setMailError]         = useState('');
  const [exporting, setExporting] = useState(false);

  // ── mobile ─────────────────────────────────────────────────────────────────
  const [isMobile, setIsMobile] = useState(() => window.matchMedia('(pointer: coarse), (max-width: 768px)').matches);
  const [mobilePickedId, setMobilePickedId] = useState<number | null>(null);
  const [activePalIdx, setActivePalIdx] = useState(0);

  // référence sur le board pour l'auto-scroll horizontal pendant le drag
  const boardRef = useRef<HTMLDivElement>(null);

  // aptitudes configurables
  const [aptitudesOptions, setAptitudesOptions] = useState<string[]>(APTITUDES_OPTIONS);

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
      const [slotData, diversData, pData, divesData] = await Promise.all([
        slotService.getById(slotId),
        slotDiverService.getBySlot(slotId),
        palanqueeService.getBySlot(slotId),
        slotDiveService.getBySlot(slotId).catch(() => [] as SlotDive[]),
      ]);
      setSlot(slotData);
      setAllDivers(diversData);
      setPalanquees(pData);
      setSlotDives(divesData);
      // Charger la liste d'attente en silence (peut échouer si non autorisé)
      try {
        const wl = await waitingListService.getWaitingList(slotId);
        setWaitingList(wl);
      } catch {
        setWaitingList([]);
      }
    } catch {
      setError('Impossible de charger les données du créneau.');
    } finally {
      setLoading(false);
    }
  }, [slotId]);

  useEffect(() => { loadAll(); }, [loadAll]);

  // Charger les aptitudes configurées
  useEffect(() => {
    adminService.getConfig().then(cfg => {
      if (cfg.aptitudes?.length) setAptitudesOptions(cfg.aptitudes);
    }).catch(() => { /* utiliser la liste de repli */ });
  }, []);

  // Suivi responsive
  useEffect(() => {
    const mq = window.matchMedia('(pointer: coarse), (max-width: 768px)');
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
    // Si une plongée est active et le plongeur est dans une palanquée de cette plongée,
    // on met à jour les aptitudes spécifiques à cette plongée (PalanqueeMember).
    // Sinon (pool ou onglet "Toutes"), on met à jour les aptitudes globales du SlotDiver.
    const palanqueeInCurrentDive = activeDiveId !== null
      ? palanquees.find(p => p.slotDiveId === activeDiveId && p.divers.some(d => d.id === diverId))
      : null;

    if (palanqueeInCurrentDive) {
      const member = palanqueeInCurrentDive.divers.find(d => d.id === diverId);
      if (member?.aptitudes === (newAptitudes || undefined)) return;
      // Mise à jour optimiste dans la palanquée uniquement
      setPalanquees(prev => prev.map(p =>
        p.id === palanqueeInCurrentDive.id
          ? { ...p, divers: p.divers.map(d => d.id === diverId ? { ...d, aptitudes: newAptitudes || undefined } : d) }
          : p
      ));
      try {
        await palanqueeService.updateMemberAptitudes(slotId, palanqueeInCurrentDive.id, diverId, newAptitudes || undefined);
      } catch { await loadAll(); }
    } else {
      // Aptitudes globales (comportement d'origine)
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
    }
  }, [allDivers, palanquees, activeDiveId, slotId, loadAll]);

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

  // ── filtrage des palanquées par plongée active ────────────────────────────
  // activeDiveId=null → toutes les palanquées
  const filteredPalanquees = activeDiveId === null
    ? palanquees
    : palanquees.filter(p => p.slotDiveId === activeDiveId);

  // ── calcul des non-assignés (pool propre à la plongée active) ─────────────
  // En mode multi-plongée, chaque plongée a son propre pool : un plongeur
  // dans dive 1 reste disponible dans le pool de dive 2.
  const assignedInCurrentDive = new Set(filteredPalanquees.flatMap(p => p.divers.map(d => d.id)));
  const unassigned  = allDivers.filter(d => !assignedInCurrentDive.has(d.id));

  // ── assignation commune (DnD + tap mobile) ───────────────────────────────
  const handleAssign = useCallback(async (
    diverId: number,
    targetPalanqueeId: number | null,
    beforeDiverId: number | null = null,
  ) => {
    // En mode plongée active, on cherche la palanquée courante uniquement dans
    // le contexte de cette plongée (un plongeur peut être dans plusieurs plongées)
    const contextPals = activeDiveId === null
      ? palanquees
      : palanquees.filter(p => p.slotDiveId === activeDiveId);
    const currentPalanquee = contextPals.find(p => p.divers.some(d => d.id === diverId));
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
      // En mode multi-plongée, passer fromPalanqueeId pour ne désassigner que de
      // cette plongée-ci (le plongeur reste dans les autres plongées)
      const fromId = targetPalanqueeId === null ? currentPalanqueeId : null;
      await palanqueeService.assign(slotId, diverId, targetPalanqueeId, fromId);
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
  }, [allDivers, palanquees, activeDiveId, slotId, loadAll]);

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
      // Auto-assigner à la plongée active si applicable
      if (activeDiveId !== null) {
        await slotDiveService.assignPalanquee(slotId, created.id, activeDiveId);
        created.slotDiveId = activeDiveId;
      }
      setPalanquees(prev => [...prev, created]);
    } catch {
      setError('Impossible de créer la palanquée.');
    } finally {
      setSaving(false);
    }
  };

  // ── plongées multiples ────────────────────────────────────────────────────

  const handleAddDive = async () => {
    setSaving(true);
    try {
      const diveNumber = slotDives.length + 1;
      const created = await slotDiveService.create(slotId, { label: `Plongée ${diveNumber}` });
      setSlotDives(prev => [...prev, created]);
      setActiveDiveId(created.id);
    } catch {
      setError('Impossible de créer la plongée.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteDive = async (diveId: number) => {
    const dive = slotDives.find(d => d.id === diveId);
    const diveLabel = dive?.label ?? `Plongée ${dive?.diveIndex}`;
    const palanqueesForDive = palanquees.filter(p => p.slotDiveId === diveId);
    if (palanqueesForDive.length > 0) {
      if (!window.confirm(`Supprimer « ${diveLabel} » ? Ses ${palanqueesForDive.length} palanquée(s) ne seront plus associées à une plongée.`)) return;
    }
    try {
      await slotDiveService.delete(slotId, diveId);
      setSlotDives(prev => prev.filter(d => d.id !== diveId).map((d, i) => ({ ...d, diveIndex: i + 1 })));
      setPalanquees(prev => prev.map(p => p.slotDiveId === diveId ? { ...p, slotDiveId: null } : p));
      if (activeDiveId === diveId) setActiveDiveId(null);
    } catch {
      setError('Impossible de supprimer la plongée.');
    }
  };

  const handleAssignPalanqueeToDive = async (palanqueeId: number, newDiveId: number | null) => {
    try {
      await slotDiveService.assignPalanquee(slotId, palanqueeId, newDiveId);
      setPalanquees(prev => prev.map(p => p.id === palanqueeId ? { ...p, slotDiveId: newDiveId } : p));
    } catch {
      await loadAll();
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

  // ── horaire de plongée ───────────────────────────────────────────────────

  const handleDiveTimeChange = useCallback(async (
    diveId: number,
    field: 'startTime' | 'endTime',
    value: string,
  ) => {
    const dive = slotDives.find(d => d.id === diveId);
    if (!dive) return;
    const updated = { ...dive, [field]: value || null };
    setSlotDives(prev => prev.map(d => d.id === diveId ? updated : d));
    try {
      await slotDiveService.update(slotId, diveId, {
        label: dive.label,
        startTime: field === 'startTime' ? (value || null) : dive.startTime,
        endTime:   field === 'endTime'   ? (value || null) : dive.endTime,
        depth:     dive.depth,
        duration:  dive.duration,
      });
    } catch { await loadAll(); }
  }, [slotDives, slotId, loadAll]);

  // ── liste d'attente ──────────────────────────────────────────────────────

  const handleApprove = useCallback(async (entryId: number) => {
    setApprovingId(entryId);
    setWlError('');
    try {
      await waitingListService.approve(slotId, entryId);
      await loadAll();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setWlError(m || 'Erreur lors de la validation');
    } finally {
      setApprovingId(null);
    }
  }, [slotId, loadAll]);

  const handleCancelEntry = useCallback(async (entryId: number) => {
    setCancelingId(entryId);
    setWlError('');
    try {
      await waitingListService.cancel(slotId, entryId);
      setWaitingList(prev => prev.filter(e => e.id !== entryId));
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setWlError(m || 'Erreur lors de la suppression');
    } finally {
      setCancelingId(null);
    }
  }, [slotId]);

  const openAttachment = useCallback(async (slotId: number, entryId: number, type: 'medical-cert' | 'license-qr') => {
    const token = localStorage.getItem('token');
    const url = waitingListService.getAttachmentUrl(slotId, entryId, type);
    try {
      const res = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) {
        alert('Impossible d\'ouvrir le fichier.');
        return;
      }
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const win = window.open(blobUrl, '_blank');
      // Libérer l'URL objet après que l'onglet l'a chargée
      if (win) {
        win.addEventListener('load', () => URL.revokeObjectURL(blobUrl), { once: true });
      }
    } catch {
      alert('Erreur lors de l\'ouverture du fichier.');
    }
  }, []);

  const handleUpdateStatus = useCallback(async (entryId: number, status: RegistrationStatus, reason?: string) => {
    setStatusUpdatingId(entryId);
    setWlError('');
    try {
      if (status === 'INCOMPLETE') {
        await waitingListService.updateStatus(slotId, entryId, status, reason);
        setWaitingList(prev => prev.filter(e => e.id !== entryId));
      } else {
        const updated = await waitingListService.updateStatus(slotId, entryId, status, reason);
        setWaitingList(prev => prev.map(e => e.id === entryId ? updated : e));
      }
      setIncompleteReasonId(null);
      setIncompleteReason('');
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setWlError(m || 'Erreur lors de la mise à jour du statut');
    } finally {
      setStatusUpdatingId(null);
    }
  }, [slotId]);

  const handleMoveToWaitingList = useCallback(async (diverId: number) => {
    if (!window.confirm('Remettre ce plongeur en liste d’attente ?')) return;
    setMovingToWlId(diverId);
    setWlError('');
    try {
      await slotDiverService.moveToWaitingList(slotId, diverId);
      await loadAll();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setWlError(m || 'Erreur lors du transfert en liste d’attente');
    } finally {
      setMovingToWlId(null);
    }
  }, [slotId, loadAll]);

  // ── export liste plongeurs CSV ───────────────────────────────────────────
  const handleExportDiverList = () => {
    if (!slot) return;
    exportDiverListCsv(slot, allDivers);
  };

  // ── export Excel ─────────────────────────────────────────────────────────
  const handleExportExcel = async () => {
    if (!slot) return;
    setExporting(true);
    try {
      const { exportFicheSecuriteAvecPalanquees } = await import('../utils/exportFicheSecuriteAvecPalanquees');
      if (slotDives.length > 0) {
        // Mode multi-plongées : exporter uniquement la liste globale des plongeurs (sans palanquées)
        await exportFicheSecuriteAvecPalanquees(slot, allDivers, []);
      } else {
        // Mode plongée unique : exporter la fiche de sécurité avec palanquées
        await exportFicheSecuriteAvecPalanquees(slot, allDivers, palanquees);
      }
    } catch (err) {
      console.error('Export palanquées :', err);
    } finally {
      setExporting(false);
    }
  };

  // ── mail d'organisation ───────────────────────────────────────────────────
  const handleOpenMailModal = async () => {
    setMailSuccess(''); setMailError('');
    // Charger le profil et la config admin pour récupérer le modèle
    let template = DpOrganizerMailer.DEFAULT_TEMPLATE;
    try {
      const [profile, cfg] = await Promise.all([
        authService.getProfile(),
        adminService.getConfig().catch(() => null),
      ]);
      if (profile.dpOrganizerEmailTemplate) template = profile.dpOrganizerEmailTemplate;
      else if (cfg?.defaultOrganizerMailTemplate) template = cfg.defaultOrganizerMailTemplate;
    } catch { /* utiliser le modèle par défaut */ }

    const titlePart = slot?.title?.trim();
    const defSubject = slot
      ? `Organisation sortie du ${fmtDate(slot.slotDate)}${titlePart ? ` — ${titlePart}` : ''}`
      : 'Organisation de la sortie';

    setMailSubject(defSubject);
    setMailBody(template);
    setEmailOverrides({});
    setMailAttachment(null);
    setMailBodyKey(k => k + 1); // force RichTextEditor remount avec le nouveau contenu
    setShowMailModal(true);
  };

  const isValidEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());

  const handleSendOrganizationMail = async () => {
    setMailSending(true); setMailError(''); setMailSuccess('');
    try {
      const result = await slotMailService.sendOrganizationMail(
        slotId, mailSubject, mailBody,
        Object.keys(emailOverrides).length ? emailOverrides : undefined,
        mailAttachment,
      );
      if (result.missingEmails.length > 0) {
        // Le serveur a retourné des emails manquants (ne devrait pas arriver si le front valide)
        const names = result.missingEmails.map(m => m.diverName).join(', ');
        setMailError(`Emails manquants pour : ${names}. Veuillez les saisir ci-dessous.`);
        const newOv: Record<number, string> = { ...emailOverrides };
        result.missingEmails.forEach(m => { if (!newOv[m.diverId]) newOv[m.diverId] = ''; });
        setEmailOverrides(newOv);
      } else {
        setMailSuccess(`Mail envoyé avec succès à ${result.sent} destinataire(s).`);
        setMailAttachment(null);
        setTimeout(() => setShowMailModal(false), 2000);
      }
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setMailError(m ?? 'Erreur lors de l\'envoi du mail.');
    } finally {
      setMailSending(false);
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
        <button className="palanquee-back-btn" onClick={() => onBack(slot?.slotDate)}>← Retour</button>
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
            className="palanquee-export-btn palanquee-export-btn--calendar"
            onClick={() => slot && downloadSlotIcs(slot)}
            disabled={!slot}
            title="Télécharger le fichier .ics pour ajouter ce créneau à votre agenda"
          >
            📅 Agenda
          </button>
          <button
            className="palanquee-export-btn"
            onClick={handleExportDiverList}
            disabled={allDivers.length === 0}
            title="Télécharger la liste des plongeurs avec emails (CSV)"
          >
            📋 Liste plongeurs
          </button>
          <button
            className="palanquee-export-btn"
            onClick={handleExportExcel}
            disabled={exporting || allDivers.length === 0}
            title={slotDives.length > 0
              ? 'Exporter la liste globale des plongeurs inscrits (Excel)'
              : 'Exporter la fiche de sécurité avec les palanquées (Excel)'}
          >
            {exporting ? '…' : slotDives.length > 0 ? '📥 Liste globale' : '📥 Fiche de sécurité'}
          </button>
          <button
            className="palanquee-export-btn palanquee-export-btn--mail"
            onClick={handleOpenMailModal}
            disabled={allDivers.length === 0}
            title="Envoyer un mail d'organisation aux plongeurs inscrits"
          >
            📧 Mail d'organisation
          </button>
        </div>
      </div>

      {error && <div className="palanquee-error">{error}</div>}

      {/* ── Liste d'attente (visible uniquement par le DP / ADMIN) ── */}
      {waitingList.length > 0 && (
        <div className="palanquee-waiting-list">
          <div className="palanquee-waiting-list-header">
            <div className="palanquee-waiting-list-title-row">
              <h3 className="palanquee-waiting-list-title">
                📋 Liste d'attente
                <span className="palanquee-waiting-list-badge">{waitingList.length}</span>
              </h3>
              <span className="palanquee-waiting-list-hint">
                Validez pour ajouter au pool · Refusez pour retirer
              </span>
            </div>
            {slot && allDivers.length >= slot.diverCount && (
              <div className="palanquee-wl-full-notice">
                ⚠️ Créneau complet ({allDivers.length}/{slot.diverCount}) — retirez un plongeur pour pouvoir valider une inscription
              </div>
            )}
          </div>
          {wlError && <div className="palanquee-error" style={{ marginBottom: 8 }}>{wlError}</div>}
          <div className="palanquee-waiting-list-entries">
            {waitingList.map((entry, idx) => {
              const regDate = new Date(entry.registeredAt).toLocaleDateString('fr-FR', {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit',
              });
              return (
                <div key={entry.id} className="palanquee-wl-entry">
                  <div className="palanquee-wl-entry-rank">#{idx + 1}</div>

                  <div className="palanquee-wl-entry-body">
                    <div className="palanquee-wl-entry-top">
                      <span className="palanquee-wl-entry-name">{entry.firstName} {entry.lastName}</span>
                      <span className="palanquee-wl-entry-level">{entry.level}</span>
                      {entry.preparedLevel && entry.preparedLevel !== 'Aucun' && (
                        <span className="palanquee-wl-entry-prep" title="Niveau en préparation">
                          → {entry.preparedLevel}
                        </span>
                      )}
                    </div>

                    <div className="palanquee-wl-entry-meta">
                      <a href={`mailto:${entry.email}`} className="palanquee-wl-entry-email" title="Envoyer un e-mail">
                        ✉️ {entry.email}
                      </a>
                      {entry.club && (
                        <span className="palanquee-wl-chip" title="Club d'appartenance">
                          🏊 {entry.club}
                        </span>
                      )}
                      {entry.numberOfDives !== undefined && entry.numberOfDives > 0 && (
                        <span className="palanquee-wl-chip" title="Nombre de plongées">
                          🤿 {entry.numberOfDives} plongée{entry.numberOfDives > 1 ? 's' : ''}
                        </span>
                      )}
                      {entry.lastDiveDate && (
                        <span className="palanquee-wl-chip" title="Dernière plongée">
                          📅 {entry.lastDiveDate}
                        </span>
                      )}
                      <span className="palanquee-wl-chip palanquee-wl-chip--date" title="Date d'inscription">
                        ⏱ {regDate}
                      </span>
                    </div>

                    {entry.comment && (
                      <div className="palanquee-wl-entry-comment">
                        💬 <em>{entry.comment}</em>
                      </div>
                    )}

                    {/* Statut de vérification */}
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginTop: 6, flexWrap: 'wrap' }}>
                      {entry.registrationStatus === 'VERIFIED' && (
                        <span style={{ background: '#dcfce7', color: '#166534', borderRadius: 4, padding: '2px 8px', fontSize: 12, fontWeight: 600 }}>
                          ✅ Dossier vérifié
                        </span>
                      )}
                      {entry.registrationStatus === 'INCOMPLETE' && (
                        <span style={{ background: '#fef3c7', color: '#92400e', borderRadius: 4, padding: '2px 8px', fontSize: 12, fontWeight: 600 }}>
                          ⚠️ Dossier incomplet
                        </span>
                      )}
                      {entry.registrationStatus === 'PENDING_VERIFICATION' && (
                        <span style={{ background: '#e0f2fe', color: '#0369a1', borderRadius: 4, padding: '2px 8px', fontSize: 12 }}>
                          🔍 Vérification en attente
                        </span>
                      )}
                      {entry.rejectionReason && entry.registrationStatus === 'INCOMPLETE' && (
                        <span style={{ color: '#92400e', fontSize: 12 }}>
                          — {entry.rejectionReason}
                        </span>
                      )}
                    </div>

                    {/* Pièces jointes */}
                    {(entry.hasMedicalCert || entry.hasLicenseQr) && (
                      <div style={{ display: 'flex', gap: 8, marginTop: 6, flexWrap: 'wrap' }}>
                        {entry.hasMedicalCert && (
                          <button
                            type="button"
                            onClick={() => openAttachment(Number(slotId), entry.id, 'medical-cert')}
                            style={{ fontSize: 12, color: '#1e40af', textDecoration: 'underline', background: 'none', border: 'none', padding: 0, cursor: 'pointer' }}
                            title="Ouvrir le certificat médical"
                          >
                            📄 Certificat médical
                          </button>
                        )}
                        {entry.hasLicenseQr && (
                          <button
                            type="button"
                            onClick={() => openAttachment(Number(slotId), entry.id, 'license-qr')}
                            style={{ fontSize: 12, color: '#1e40af', textDecoration: 'underline', background: 'none', border: 'none', padding: 0, cursor: 'pointer' }}
                            title="Ouvrir le QR code de la licence"
                          >
                            🪪 Licence FFESSM
                          </button>
                        )}
                      </div>
                    )}

                    {/* Formulaire motif incomplet */}
                    {incompleteReasonId === entry.id && (
                      <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
                        <textarea
                          value={incompleteReason}
                          onChange={e => setIncompleteReason(e.target.value)}
                          placeholder="Motif (ex : certificat médical illisible…)"
                          rows={2}
                          style={{ resize: 'vertical', fontSize: 13 }}
                        />
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button
                            className="btn-wl-cancel"
                            style={{ background: '#d97706', borderColor: '#d97706', color: '#fff' }}
                            disabled={statusUpdatingId === entry.id}
                            onClick={() => handleUpdateStatus(entry.id, 'INCOMPLETE', incompleteReason.trim() || undefined)}
                          >
                            {statusUpdatingId === entry.id ? '…' : '⚠️ Confirmer incomplet'}
                          </button>
                          <button
                            className="btn-wl-cancel"
                            onClick={() => { setIncompleteReasonId(null); setIncompleteReason(''); }}
                          >
                            Annuler
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="palanquee-wl-entry-actions">
                    <button
                      className="btn-wl-approve"
                      disabled={approvingId === entry.id || (slot !== null && allDivers.length >= slot.diverCount)}
                      onClick={() => handleApprove(entry.id)}
                      title={slot && allDivers.length >= slot.diverCount
                        ? "Créneau complet — retirez un plongeur d'abord"
                        : "Valider — ajoute le plongeur dans la liste des inscrits"}
                    >
                      {approvingId === entry.id ? '…' : '✓ Valider'}
                    </button>
                    {(entry.hasMedicalCert || entry.hasLicenseQr) && entry.registrationStatus !== 'VERIFIED' && (
                      <button
                        className="btn-wl-approve"
                        style={{ background: '#16a34a', borderColor: '#16a34a' }}
                        disabled={statusUpdatingId === entry.id}
                        onClick={() => handleUpdateStatus(entry.id, 'VERIFIED')}
                        title="Marquer le dossier comme vérifié et valide"
                      >
                        {statusUpdatingId === entry.id ? '…' : '✅ Dossier OK'}
                      </button>
                    )}
                    {(entry.hasMedicalCert || entry.hasLicenseQr) && entry.registrationStatus !== 'INCOMPLETE' && (
                      <button
                        className="btn-wl-cancel"
                        style={{ background: '#d97706', borderColor: '#d97706', color: '#fff' }}
                        disabled={statusUpdatingId === entry.id}
                        onClick={() => { setIncompleteReasonId(entry.id); setIncompleteReason(''); }}
                        title="Marquer le dossier comme incomplet (avec motif)"
                      >
                        ⚠️ Incomplet
                      </button>
                    )}
                    <button
                      className="btn-wl-cancel"
                      disabled={cancelingId === entry.id}
                      onClick={() => handleCancelEntry(entry.id)}
                      title="Refuser et retirer de la liste d'attente"
                    >
                      {cancelingId === entry.id ? '…' : '✕ Refuser'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Onglets plongées multiples ── */}
      {slotDives.length > 0 && (
        <div className="dive-tabs">
          <button
            className={`dive-tab${activeDiveId === null ? ' dive-tab--active' : ''}`}
            onClick={() => setActiveDiveId(null)}
          >
            🌊 Toutes
          </button>
          {slotDives.map(dive => (
            <span key={dive.id} className={`dive-tab-wrapper${activeDiveId === dive.id ? ' dive-tab-wrapper--active' : ''}`}>
              <button
                className={`dive-tab${activeDiveId === dive.id ? ' dive-tab--active' : ''}`}
                onClick={() => setActiveDiveId(dive.id)}
              >
                🤿 {dive.label ?? `Plongée ${dive.diveIndex}`}
              </button>
              <button
                className="dive-tab-export"
                disabled={exporting}
                title={`Exporter la fiche de sécurité — ${dive.label ?? `Plongée ${dive.diveIndex}`}`}
                onClick={async () => {
                  if (!slot) return;
                  setExporting(true);
                  try {
                    const { exportFicheSecuriteAvecPalanquees } = await import('../utils/exportFicheSecuriteAvecPalanquees');
                    const pals = palanquees.filter(p => p.slotDiveId === dive.id);
                    const label = dive.label ?? `Plongée ${dive.diveIndex}`;
                    await exportFicheSecuriteAvecPalanquees(slot, allDivers, pals, label, dive.startTime, dive.endTime);
                  } catch (err) { console.error(err); }
                  finally { setExporting(false); }
                }}
              >{exporting ? '…' : '📥'}</button>
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

      {slotDives.length === 0 && (
        <div className="dive-tabs-empty">
          <button
            className="dive-tab dive-tab--add"
            onClick={handleAddDive}
            disabled={saving}
            title="Organiser ce créneau en plusieurs plongées (matin/après-midi…)"
          >
            + Organiser en plusieurs plongées
          </button>
        </div>
      )}

      {activeDiveId !== null && slotDives.length > 0 && (() => {
        const dive = slotDives.find(d => d.id === activeDiveId);
        if (!dive) return null;
        return (
          <div className="dive-time-bar">
            <span className="dive-time-bar-label">🕐 Horaire :</span>
            <input
              type="time"
              className="dive-time-input"
              value={dive.startTime?.slice(0, 5) ?? ''}
              onChange={e => handleDiveTimeChange(dive.id, 'startTime', e.target.value)}
              title="Heure de début de la plongée"
            />
            <span className="dive-time-bar-sep">—</span>
            <input
              type="time"
              className="dive-time-input"
              value={dive.endTime?.slice(0, 5) ?? ''}
              onChange={e => handleDiveTimeChange(dive.id, 'endTime', e.target.value)}
              title="Heure de fin de la plongée"
            />
          </div>
        );
      })()}

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
          onMoveToWaitingList={isRegistrationCurrentlyActive(slot) ? handleMoveToWaitingList : undefined}
          movingToWlId={movingToWlId}
          aptitudesOptions={aptitudesOptions}
        />
      </div>

      {/* ── Mobile : navigation + palanquée active ── */}
      {isMobile && (
        <div className="palanquee-mobile-view">
          {filteredPalanquees.length === 0 ? (
            <div className="palanquee-empty-state">
              <p>Aucune palanquée créée{activeDiveId !== null ? ' pour cette plongée' : ''}.</p>
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
                  {filteredPalanquees.map((_, i) => (
                    <button
                      key={i}
                      className={`palanquee-mobile-nav-dot${i === activePalIdx ? ' palanquee-mobile-nav-dot--active' : ''}`}
                      onClick={() => setActivePalIdx(i)}
                    />
                  ))}
                </div>
                <button
                  className="palanquee-mobile-nav-btn"
                  onClick={() => setActivePalIdx(i => Math.min(filteredPalanquees.length - 1, i + 1))}
                  disabled={activePalIdx >= filteredPalanquees.length - 1}
                >›</button>
              </div>

              {/* Palanquée active */}
              {filteredPalanquees[activePalIdx] && (() => {
                const p = filteredPalanquees[activePalIdx];
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
                        <div className="palanquee-column-header-top">
                          <span
                            className="palanquee-column-name"
                            title="Appui long pour renommer"
                            onDoubleClick={() => startRename(p)}
                          >
                            P{idx + 1} – {p.name}
                          </span>
                          <button
                            className="palanquee-delete-btn"
                            onClick={() => handleDeletePalanquee(p.id)}
                            title="Supprimer cette palanquée"
                          >✕</button>
                        </div>
                      )}
                      <div className="palanquee-column-params">
                        {slotDives.length > 0 && (
                          <select
                            className="palanquee-param-select palanquee-param-select--dive"
                            value={p.slotDiveId ?? ''}
                            onChange={e => handleAssignPalanqueeToDive(p.id, e.target.value ? Number(e.target.value) : null)}
                            title="Plongée associée"
                          >
                            <option value="">Plongée ▾</option>
                            {slotDives.map(d => (
                              <option key={d.id} value={d.id}>{d.label ?? `Plongée ${d.diveIndex}`}</option>
                            ))}
                          </select>
                        )}
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
                      onMoveToWaitingList={isRegistrationCurrentlyActive(slot) ? handleMoveToWaitingList : undefined}
                      movingToWlId={movingToWlId}
                      aptitudesOptions={aptitudesOptions}
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
          {filteredPalanquees.map((p, idx) => (
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
                  <div className="palanquee-column-header-top">
                    <span
                      className="palanquee-column-name"
                      title="Double-clic pour renommer"
                      onDoubleClick={() => startRename(p)}
                    >
                      P{idx + 1} – {p.name}
                    </span>
                    <button
                      className="palanquee-delete-btn"
                      onClick={() => handleDeletePalanquee(p.id)}
                      title="Supprimer cette palanquée"
                    >✕</button>
                  </div>
                )}
                <div className="palanquee-column-params">
                  {slotDives.length > 0 && (
                    <select
                      className="palanquee-param-select palanquee-param-select--dive"
                      value={p.slotDiveId ?? ''}
                      onChange={e => handleAssignPalanqueeToDive(p.id, e.target.value ? Number(e.target.value) : null)}
                      title="Plongée associée"
                    >
                      <option value="">Plongée ▾</option>
                      {slotDives.map(d => (
                        <option key={d.id} value={d.id}>{d.label ?? `Plongée ${d.diveIndex}`}</option>
                      ))}
                    </select>
                  )}
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
                onMoveToWaitingList={isRegistrationCurrentlyActive(slot) ? handleMoveToWaitingList : undefined}
                movingToWlId={movingToWlId}
                aptitudesOptions={aptitudesOptions}
              />
            </div>
          ))}

          {filteredPalanquees.length === 0 && (
            <div className="palanquee-column palanquee-column--empty">
              <div className="palanquee-empty-state">
                <p>Aucune palanquée créée{activeDiveId !== null ? ' pour cette plongée' : ''}.</p>
                <button className="palanquee-add-btn" onClick={handleAddPalanquee} disabled={saving}>
                  + Créer la première palanquée
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Résumé bas de page */}
      {filteredPalanquees.length > 0 && (
        <div className="palanquee-summary">
          {filteredPalanquees.map((p, idx) => (
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
        const isInPool = !filteredPalanquees.some(p => p.divers.some(d => d.id === mobilePickedId));
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
                  📋 Non assignés
                </button>
              )}
              {filteredPalanquees.map((p, idx) => (
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

      {/* ── Modal mail d'organisation DP ─────────────────────────────────── */}
      {showMailModal && (
        <div className="modal-overlay" onClick={() => !mailSending && setShowMailModal(false)}>
          <div className="modal modal--wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>📧 Mail d'organisation</h3>
              <button className="modal-close" onClick={() => setShowMailModal(false)} disabled={mailSending}>✕</button>
            </div>

            <div className="modal-body" style={{ gap: 14 }}>
              {/* Emails manquants */}
              {(() => {
                const missingDivers = allDivers.filter(d => !d.email);
                if (missingDivers.length === 0) return null;
                return (
                  <div className="mail-modal-missing">
                    <strong>⚠️ Emails manquants — saisir manuellement :</strong>
                    {missingDivers.map(d => {
                        const val = emailOverrides[d.id] ?? '';
                        const invalid = val.trim() !== '' && !isValidEmail(val);
                        return (
                          <div key={d.id} className="mail-modal-missing-row">
                            <span>{d.firstName} {d.lastName}</span>
                            <input
                              type="email"
                              placeholder="email@example.com"
                              value={val}
                              onChange={e => setEmailOverrides(prev => ({ ...prev, [d.id]: e.target.value }))}
                              style={invalid ? { borderColor: '#ef4444' } : undefined}
                            />
                            {invalid && <span className="mail-missing-error">Format invalide</span>}
                          </div>
                        );
                      })}
                  </div>
                );
              })()}

              {/* Objet */}
              <div className="form-group">
                <label style={{ fontSize: 13 }}>Objet du mail</label>
                <input
                  type="text"
                  value={mailSubject}
                  onChange={e => setMailSubject(e.target.value)}
                  style={{ width: '100%' }}
                />
              </div>

              {/* Variables disponibles */}
              <div className="mail-modal-vars">
                <span style={{ fontSize: 12, color: '#6b7280', marginRight: 6 }}>Variables :</span>
                {['{siteName}', '{slotDate}', '{startTime}', '{endTime}', '{slotTitle}',
                  '{dpName}', '{dpEmail}', '{dpPhone}'].map(v => (
                  <span key={v} className="mail-modal-var-chip" title="Cliquez pour copier"
                    onClick={() => navigator.clipboard.writeText(v).catch(() => {})}
                  >{v}</span>
                ))}
              </div>

              {/* Éditeur WYSIWYG */}
              <div className="form-group">
                <label style={{ fontSize: 13 }}>Corps du mail</label>
                <RichTextEditor
                  key={mailBodyKey}
                  initialValue={mailBody}
                  onChange={setMailBody}
                  minHeight={380}
                />
              </div>

              {/* Pièce jointe */}
              <div className="form-group">
                <label style={{ fontSize: 13 }}>Pièce jointe <span style={{ color: '#9ca3af' }}>(optionnelle, max 3 Mo)</span></label>
                {mailAttachment ? (
                  <div className="mail-modal-attachment">
                    <span className="mail-modal-attachment-name">📎 {mailAttachment.name}</span>
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => setMailAttachment(null)}
                    >✕ Retirer</button>
                  </div>
                ) : (
                  <input
                    type="file"
                    onChange={e => {
                      const f = e.target.files?.[0] ?? null;
                      if (f && f.size > 3 * 1024 * 1024) {
                        setMailError('La pièce jointe dépasse la taille maximale autorisée (3 Mo).');
                        e.target.value = '';
                      } else {
                        setMailError('');
                        setMailAttachment(f);
                      }
                    }}
                  />
                )}
              </div>

              {/* Récapitulatif destinataires */}
              <div style={{ fontSize: 12, color: '#6b7280' }}>
                {(() => {
                  const count = allDivers.filter(
                    d => (d.email && d.email.trim()) || (emailOverrides[d.id] ?? '').trim(),
                  ).length;
                  return `${count} / ${allDivers.length} plongeur(s) ont une adresse mail · Vous serez en CC`;
                })()}
              </div>

              {mailError   && <div className="alert alert-error">{mailError}</div>}
              {mailSuccess && <div className="alert alert-success">{mailSuccess}</div>}
            </div>

            <div className="modal-footer">
              <button className="btn btn-outline" onClick={() => { setShowMailModal(false); setMailAttachment(null); }} disabled={mailSending}>
                Annuler
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSendOrganizationMail}
                disabled={
                  mailSending ||
                  !mailSubject.trim() ||
                  !mailBody.trim() ||
                  allDivers.some(d => !d.email && !(emailOverrides[d.id] ?? '').trim()) ||
                  Object.values(emailOverrides).some(v => v.trim() !== '' && !isValidEmail(v))
                }
              >
                {mailSending ? '⏳ Envoi…' : '📧 Envoyer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
