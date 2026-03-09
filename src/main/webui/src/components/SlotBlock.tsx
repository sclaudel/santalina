import { useState, useRef, useCallback, useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { AppConfig, DiveSlot, SlotDiver, SlotDiverRequest, UserSearchResult } from '../types';
import { slotDiverService } from '../services/slotDiverService';
import { slotService } from '../services/slotService';
import { adminService } from '../services/adminService';
import { getSlotTypeStyle } from '../utils/slotTypeColors';
import { exportFicheSecurite } from '../utils/exportFicheSecurite';

/** Réinitialise le zoom iOS après fermeture d'un formulaire.
 *  iOS Safari zoome automatiquement sur les inputs < 16px et ne dézoome jamais.
 *  Ce hack ajoute brièvement maximum-scale=1 pour forcer le retour à l'échelle 1. */
function resetIOSZoom() {
  const viewport = document.querySelector<HTMLMetaElement>('meta[name="viewport"]');
  if (!viewport) return;
  const original = viewport.content;
  viewport.content = original + ', maximum-scale=1';
  setTimeout(() => { viewport.content = original; }, 300);
}

interface Props {
  slot: DiveSlot;
  maxDivers?: number;
  config?: AppConfig;
  height: number;
  onDelete: (id: number) => void;
  onRefresh: () => void;
  canEdit: boolean;
  currentUserId?: number;
  currentUserRole?: string;
}

const LEVEL_COLORS: Record<string, string> = {
  // Niveaux fédéraux
  'E1':                   '#93c5fd',
  'E2':                   '#60a5fa',
  'Niveau 1':             '#3b82f6',
  'Niveau 2':             '#10b981',
  'Niveau 3':             '#f59e0b',
  'Niveau 4':             '#8b5cf6',
  'Guide de Palanquée':   '#0ea5e9',
  // Monitorat
  'MF1':                  '#ef4444',
  'MF2':                  '#dc2626',
  'Moniteur':             '#ef4444',
  'Directeur de plongée': '#991b1b',
  // PADI
  'PADI Open Water':      '#3b82f6',
  'PADI Advanced':        '#10b981',
  'PADI Rescue':          '#f59e0b',
  // Préparations
  'Prepa-N1':             '#bfdbfe',
  'Prepa-N2':             '#a7f3d0',
  'Prepa-N3':             '#fde68a',
  'Prepa-N4':             '#ddd6fe',
  'Prepa-MF1':            '#fca5a5',
  'Prepa-MF2':            '#f87171',
};

const TOOLTIP_WIDTH = 320;


const EMPTY_FORM: SlotDiverRequest = {
  firstName: '', lastName: '', level: 'Inconnu',
  email: '', phone: '', isDirector: false,
};

function getLevelColor(level: string): string {
  return LEVEL_COLORS[level] ?? '#6b7280';
}

function getCapacityColor(used: number, max: number): string {
  const pct = used / max;
  if (pct >= 1)   return '#ef4444';
  if (pct >= 0.8) return '#f59e0b';
  return '#10b981';
}

function timeOptions(resolutionMinutes: number): string[] {
  const res = (resolutionMinutes > 0 && isFinite(resolutionMinutes)) ? resolutionMinutes : 15;
  const opts: string[] = [];
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += res) {
      opts.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
    }
  }
  return opts;
}

export function SlotBlock({
  slot, height, onDelete, onRefresh,
  canEdit, currentUserId, currentUserRole, maxDivers = 25, config,
}: Props) {
  const [showTooltip, setShowTooltip]         = useState(false);
  const [showDiverForm, setShowDiverForm]     = useState(false);
  const [tooltipStyle, setTooltipStyle]       = useState<React.CSSProperties>({});
  const [showHoverTooltip, setShowHoverTooltip] = useState(false);
  const [hoverTooltipStyle, setHoverTooltipStyle] = useState<React.CSSProperties>({});
  const [divers, setDivers]               = useState<SlotDiver[]>(slot.divers ?? []);
  const [form, setForm]                   = useState<SlotDiverRequest>(EMPTY_FORM);
  const [saving, setSaving]               = useState(false);
  const [error, setError]                 = useState('');
  const [addSuccess, setAddSuccess]        = useState('');
  const [loading, setLoading]             = useState(false);
  // Édition d'un plongeur existant
  const [editingDiver, setEditingDiver]   = useState<SlotDiver | null>(null);
  const [editForm, setEditForm]           = useState<SlotDiverRequest>(EMPTY_FORM);
  const [editSaving, setEditSaving]       = useState(false);
  const [editError, setEditError]         = useState('');
  // Édition du nombre de plongeurs du créneau
  const [editingDiverCount, setEditingDiverCount] = useState(false);
  const [diverCountStr, setDiverCountStr]         = useState(String(slot.diverCount));
  const [diverCountError, setDiverCountError]     = useState('');
  const [diverCountSaving, setDiverCountSaving]   = useState(false);
  const [currentDiverCount, setCurrentDiverCount] = useState(slot.diverCount);
  // Édition des infos du créneau (titre, notes, type, club)
  const [editingInfo, setEditingInfo]   = useState(false);
  const [infoTitle, setInfoTitle]       = useState(slot.title ?? '');
  const [infoNotes, setInfoNotes]       = useState(slot.notes ?? '');
  const [infoSlotType, setInfoSlotType] = useState(slot.slotType ?? '');
  const [infoClub, setInfoClub]         = useState(slot.club ?? '');
  const [infoSlotDate, setInfoSlotDate]   = useState(slot.slotDate);
  const [infoStartTime, setInfoStartTime] = useState(slot.startTime);
  const [infoEndTime, setInfoEndTime]     = useState(slot.endTime);
  const [infoSaving, setInfoSaving]     = useState(false);
  const [infoError, setInfoError]       = useState('');
  // Valeurs courantes affichées (mises à jour après sauvegarde)
  const [currentTitle, setCurrentTitle]     = useState(slot.title ?? '');
  const [currentNotes, setCurrentNotes]     = useState(slot.notes ?? '');
  const [currentSlotType, setCurrentSlotType] = useState(slot.slotType ?? '');
  const [currentClub, setCurrentClub]       = useState(slot.club ?? '');

  // Recherche utilisateur (formulaire ajout)
  const [searchQuery, setSearchQuery]         = useState('');
  const [searchResults, setSearchResults]     = useState<UserSearchResult[]>([]);
  const [searchLoading, setSearchLoading]     = useState(false);
  // Recherche utilisateur (formulaire édition)
  const [editSearchQuery, setEditSearchQuery]     = useState('');
  const [editSearchResults, setEditSearchResults] = useState<UserSearchResult[]>([]);
  const [editSearchLoading, setEditSearchLoading] = useState(false);

  const times = timeOptions(config?.slotResolutionMinutes ?? 15);

  const blockRef   = useRef<HTMLDivElement>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  const canEditThisSlot = canEdit && (
    currentUserRole === 'ADMIN' ||
    (currentUserRole === 'DIVE_DIRECTOR' && slot.createdById === currentUserId)
  );

  const usedDivers     = divers.length;
  const color          = getCapacityColor(usedDivers, currentDiverCount);
  const isCompact      = height < 60;
  const hasDirector    = divers.some(d => d.isDirector);

  // Recherche utilisateur avec debounce (formulaire ajout)
  useEffect(() => {
    if (!searchQuery.trim()) { setSearchResults([]); return; }
    const t = setTimeout(async () => {
      setSearchLoading(true);
      try { setSearchResults(await adminService.searchUsers(searchQuery)); }
      catch { setSearchResults([]); }
      finally { setSearchLoading(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  // Recherche utilisateur avec debounce (formulaire édition)
  useEffect(() => {
    if (!editSearchQuery.trim()) { setEditSearchResults([]); return; }
    const t = setTimeout(async () => {
      setEditSearchLoading(true);
      try { setEditSearchResults(await adminService.searchUsers(editSearchQuery)); }
      catch { setEditSearchResults([]); }
      finally { setEditSearchLoading(false); }
    }, 300);
    return () => clearTimeout(t);
  }, [editSearchQuery]);

  // Calcule position fixed en tenant compte de l'espace disponible
  const computePos = useCallback(() => {
    if (!blockRef.current) return;
    const rect       = blockRef.current.getBoundingClientRect();
    const spaceRight = window.innerWidth - rect.right;
    const maxH       = Math.min(520, window.innerHeight - 24);
    const top        = Math.max(8, Math.min(rect.top, window.innerHeight - maxH - 8));

    if (spaceRight >= TOOLTIP_WIDTH + 12) {
      setTooltipStyle({ position: 'fixed', top, left: rect.right + 8, width: TOOLTIP_WIDTH, maxHeight: maxH, overflowY: 'auto', zIndex: 99999 });
    } else {
      setTooltipStyle({ position: 'fixed', top, right: window.innerWidth - rect.left + 8, left: 'auto', width: TOOLTIP_WIDTH, maxHeight: maxH, overflowY: 'auto', zIndex: 99999 });
    }
  }, []);

  // Calcule position du tooltip de survol
  const computeHoverPos = useCallback(() => {
    if (!blockRef.current) return;
    const rect       = blockRef.current.getBoundingClientRect();
    const spaceRight = window.innerWidth - rect.right;
    const maxH       = Math.min(600, window.innerHeight - 24);
    const top        = Math.max(8, Math.min(rect.top, window.innerHeight - maxH - 8));

    if (spaceRight >= TOOLTIP_WIDTH + 12) {
      setHoverTooltipStyle({ position: 'fixed', top, left: rect.right + 8, width: TOOLTIP_WIDTH, maxHeight: maxH, overflowY: 'auto', zIndex: 99998 });
    } else {
      setHoverTooltipStyle({ position: 'fixed', top, right: window.innerWidth - rect.left + 8, left: 'auto', width: TOOLTIP_WIDTH, maxHeight: maxH, overflowY: 'auto', zIndex: 99998 });
    }
  }, []);

  const handleMouseEnter = useCallback(() => {
    if (showTooltip) return;
    if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
    computeHoverPos();
    setShowHoverTooltip(true);
  }, [showTooltip, computeHoverPos]);

  const handleMouseLeave = useCallback(() => {
    setShowHoverTooltip(false);
  }, []);

  // Clic sur le bloc : ouvre / ferme
  const handleBlockClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowHoverTooltip(false);
    if (showTooltip) {
      setShowTooltip(false);
      setShowDiverForm(false);
      return;
    }
    computePos();
    setShowTooltip(true);
    setLoading(true);
    try {
      const fresh = await slotDiverService.getBySlot(slot.id);
      setDivers(fresh);
    } catch { /* silencieux */ }
    finally { setLoading(false); }
  };

  // Clic en dehors → fermer
  useEffect(() => {
    if (!showTooltip) return;
    const onMouseDown = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        !tooltipRef.current?.contains(target) &&
        !blockRef.current?.contains(target)
      ) {
        setShowTooltip(false);
        setShowDiverForm(false);
      }
    };
    // Petit délai pour éviter que le clic d'ouverture ferme tout de suite
    const t = setTimeout(() => document.addEventListener('mousedown', onMouseDown), 10);
    return () => { clearTimeout(t); document.removeEventListener('mousedown', onMouseDown); };
  }, [showTooltip]);

  const closeTooltip = () => {
    setShowTooltip(false); setShowDiverForm(false);
    setError(''); setEditingDiver(null); setEditError('');
    setEditingDiverCount(false); setDiverCountError('');
    setEditingInfo(false); setInfoError('');
  };

  const startEditInfo = () => {
    setInfoTitle(currentTitle);
    setInfoNotes(currentNotes);
    setInfoSlotType(currentSlotType);
    setInfoClub(currentClub);
    setInfoSlotDate(slot.slotDate);
    setInfoStartTime(slot.startTime);
    setInfoEndTime(slot.endTime);
    setInfoError('');
    setEditingInfo(true);
    setEditingDiverCount(false);
  };

  const handleUpdateSlotInfo = async (e: React.FormEvent) => {
    e.preventDefault();
    setInfoSaving(true); setInfoError('');
    try {
      await slotService.updateSlotInfo(slot.id, {
        title: infoTitle || undefined,
        notes: infoNotes || undefined,
        slotType: infoSlotType || undefined,
        club: infoClub || undefined,
        slotDate: infoSlotDate || undefined,
        startTime: infoStartTime || undefined,
        endTime: infoEndTime || undefined,
      });
      setCurrentTitle(infoTitle);
      setCurrentNotes(infoNotes);
      setCurrentSlotType(infoSlotType);
      setCurrentClub(infoClub);
      setEditingInfo(false);
      onRefresh();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setInfoError(m || 'Erreur lors de la modification');
    } finally { setInfoSaving(false); }
  };

  const handleUpdateDiverCount = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseInt(diverCountStr, 10);
    if (!diverCountStr || isNaN(val) || val < 1) {
      setDiverCountError('La valeur doit être au moins 1');
      return;
    }
    if (val > maxDivers) {
      setDiverCountError(`La valeur ne peut pas dépasser ${maxDivers}`);
      return;
    }
    if (val < usedDivers) {
      setDiverCountError(`Impossible : ${usedDivers} plongeur(s) déjà inscrits`);
      return;
    }
    setDiverCountSaving(true); setDiverCountError('');
    try {
      await slotService.updateDiverCount(slot.id, val);
      setCurrentDiverCount(val);
      setEditingDiverCount(false);
      onRefresh();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setDiverCountError(m || 'Erreur lors de la modification');
    } finally { setDiverCountSaving(false); }
  };

  const startEdit = (d: SlotDiver) => {
    setEditingDiver(d);
    setEditForm({ firstName: d.firstName, lastName: d.lastName, level: d.level,
                  email: d.email ?? '', phone: d.phone ?? '', isDirector: d.isDirector });
    setEditSearchQuery(''); setEditSearchResults([]);
    setEditError('');
    setShowDiverForm(false);
  };

  /** Pré-remplit le formulaire d'ajout depuis un utilisateur trouvé */
  const selectUserForAdd = (u: UserSearchResult) => {
    const parts = u.name.trim().split(' ');
    const firstName = parts[0] ?? '';
    const lastName  = parts.slice(1).join(' ') || (parts[0] ?? '');
    setForm(f => ({ ...f, firstName, lastName, email: u.email ?? '', phone: u.phone ?? '' }));
    setSearchQuery(''); setSearchResults([]);
  };

  /** Pré-remplit le formulaire d'édition depuis un utilisateur trouvé */
  const selectUserForEdit = (u: UserSearchResult) => {
    const parts = u.name.trim().split(' ');
    const firstName = parts[0] ?? '';
    const lastName  = parts.slice(1).join(' ') || (parts[0] ?? '');
    setEditForm(f => ({ ...f, firstName, lastName, email: u.email ?? '', phone: u.phone ?? '' }));
    setEditSearchQuery(''); setEditSearchResults([]);
  };

  const handleUpdateDiver = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDiver) return;
    if (editForm.isDirector) {
      if (!editForm.email?.trim()) { setEditError("L'email est obligatoire pour un directeur de plongée"); return; }
      if (!editForm.phone?.trim()) { setEditError("Le téléphone est obligatoire pour un directeur de plongée"); return; }
    }
    setEditSaving(true); setEditError('');
    try {
      const updated = await slotDiverService.update(slot.id, editingDiver.id, {
        ...editForm,
      });
      setDivers(prev => {
        const next = prev.map(d => d.id === updated.id ? updated : d);
        return next.sort((a, b) => (b.isDirector ? 1 : 0) - (a.isDirector ? 1 : 0)
          || a.lastName.localeCompare(b.lastName));
      });
      setEditingDiver(null);
      onRefresh();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setEditError(m || "Erreur lors de la modification");
    } finally { setEditSaving(false); }
  };

  const handleAddDiver = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.isDirector) {
      if (!form.email?.trim()) { setError("L'email est obligatoire pour un directeur de plongée"); return; }
      if (!form.phone?.trim()) { setError("Le téléphone est obligatoire pour un directeur de plongée"); return; }
      if (hasDirector)         { setError("Il y a déjà un directeur de plongée sur ce créneau"); return; }
    }
    setSaving(true); setError(''); setAddSuccess('');
    try {
      const d = await slotDiverService.add(slot.id, form);
      setDivers(prev => d.isDirector ? [d, ...prev] : [...prev, d]);
      setForm(EMPTY_FORM);
      setAddSuccess(`✓ ${d.firstName} ${d.lastName} ajouté(e) avec succès`);
      setTimeout(() => setAddSuccess(''), 3000);
      onRefresh();
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m || "Erreur lors de l'ajout");
    } finally { setSaving(false); }
  };

  const handleRemoveDiver = async (diverId: number) => {
    try {
      await slotDiverService.remove(slot.id, diverId);
      setDivers(prev => prev.filter(d => d.id !== diverId));
      onRefresh();
    } catch { /* silencieux */ }
  };

  // ---- Hover tooltip (desktop uniquement, lecture seule) ----
  const hoverTooltipContent = (showHoverTooltip && !showTooltip) ? createPortal(
    <div className="slot-hover-tooltip" style={hoverTooltipStyle}>
      <div className="slot-hover-tooltip-header">
        <span className="slot-hover-tooltip-title">{currentTitle || 'Créneau de plongée'}</span>
        <span className="slot-hover-tooltip-time">{slot.startTime}–{slot.endTime}</span>
      </div>
      <div className="slot-hover-tooltip-capacity" style={{ color }}>
        🤿 {usedDivers}/{currentDiverCount} plongeur{usedDivers !== 1 ? 's' : ''}
      </div>
      {(currentSlotType || currentClub) && (
        <div className="slot-tooltip-tags" style={{ marginBottom: 8 }}>
          {currentSlotType && (
            <span className="slot-tag" style={{ background: getSlotTypeStyle(currentSlotType).tagBg, color: getSlotTypeStyle(currentSlotType).tagColor }}>
              {currentSlotType}
            </span>
          )}
          {currentClub && <span className="slot-tag slot-tag-club">🏊 {currentClub}</span>}
        </div>
      )}
      {divers.length === 0 ? (
        <p className="slot-tooltip-empty">Aucun plongeur inscrit</p>
      ) : (
        <ul className="slot-hover-diver-list">
          {divers.map(d => (
            <li key={d.id} className={`slot-hover-diver-item${d.isDirector ? ' slot-hover-diver-director' : ''}`}>
              {d.isDirector && <span className="diver-director-badge" title="Directeur de plongée">🎖</span>}
              <span className="diver-level-dot" style={{ background: getLevelColor(d.level) }} title={d.level} />
              <span className="slot-hover-diver-name">{d.firstName} {d.lastName}</span>
              <span className="slot-hover-diver-level">{d.level}</span>
            </li>
          ))}
        </ul>
      )}
      <div className="slot-hover-tooltip-hint">Cliquer pour gérer le créneau</div>
    </div>,
    document.body
  ) : null;

  // ---- Tooltip rendu via Portal (hors de tout parent overflow:hidden) ----
  const tooltipContent = showTooltip ? createPortal(
    <div
      ref={tooltipRef}
      className="slot-tooltip"
      style={tooltipStyle}
      onMouseDown={e => e.stopPropagation()}
    >
      {/* En-tête */}
      <div className="slot-tooltip-header">
        <span className="slot-tooltip-title">{currentTitle || 'Créneau de plongée'}</span>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexShrink: 0 }}>
          <span className="slot-tooltip-time">{slot.startTime}–{slot.endTime}</span>
          <button className="slot-tooltip-close" onClick={closeTooltip} title="Fermer">✕</button>
        </div>
      </div>

      {/* Infos générales */}
      <div className="slot-tooltip-info">
        <span>👤 {slot.createdByName}</span>
        <span style={{ color }}>🤿 {usedDivers}/{currentDiverCount}</span>
        {canEditThisSlot && !editingDiverCount && (
          <button className="btn-edit-diver-count" title="Modifier la capacité"
            onClick={() => { setDiverCountStr(String(currentDiverCount)); setEditingDiverCount(true); setDiverCountError(''); setEditingInfo(false); }}
          >✏️ Capacité</button>
        )}
      </div>

      {/* Tags type / club */}
      {(currentSlotType || currentClub) && !editingInfo && (
        <div className="slot-tooltip-tags">
          {currentSlotType && (
            <span className="slot-tag" style={{ background: getSlotTypeStyle(currentSlotType).tagBg, color: getSlotTypeStyle(currentSlotType).tagColor }}>
              {currentSlotType}
            </span>
          )}
          {currentClub && <span className="slot-tag slot-tag-club">🏊 {currentClub}</span>}
        </div>
      )}

      {/* Formulaire d'édition du nombre de plongeurs */}
      {editingDiverCount && (
        <form onSubmit={handleUpdateDiverCount} className="diver-count-edit-form">
          {diverCountError && <div className="diver-form-error">{diverCountError}</div>}
          <div className="diver-count-edit-row">
            <label>Capacité (max {maxDivers}, min {usedDivers} inscrits)</label>
            <div className="diver-count-edit-inputs">
              <input type="number" min={usedDivers || 1} max={maxDivers}
                value={diverCountStr} onChange={e => setDiverCountStr(e.target.value)}
                onBlur={() => { const val = parseInt(diverCountStr, 10); if (!diverCountStr || isNaN(val) || val < 1) setDiverCountStr(String(usedDivers || 1)); }}
                autoFocus />
              <button type="submit" disabled={diverCountSaving} className="btn-diver-save">{diverCountSaving ? '...' : '✓'}</button>
              <button type="button" className="btn-diver-cancel" onClick={() => { setEditingDiverCount(false); setDiverCountError(''); }}>✕</button>
            </div>
          </div>
        </form>
      )}

      {/* Formulaire d'édition des infos du créneau */}
      {editingInfo ? (
        <form onSubmit={handleUpdateSlotInfo} className="slot-info-edit-form">
          {infoError && <div className="diver-form-error">{infoError}</div>}
          <div className="slot-info-field">
            <label>Date</label>
            <input type="date" value={infoSlotDate} onChange={e => setInfoSlotDate(e.target.value)} required />
          </div>
          <div className="slot-info-field">
            <label>Heure de début</label>
            <select value={infoStartTime} onChange={e => setInfoStartTime(e.target.value)}>
              {times.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className="slot-info-field">
            <label>Heure de fin</label>
            <select value={infoEndTime} onChange={e => setInfoEndTime(e.target.value)}>
              {times.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          {(config?.slotTypes ?? []).length > 0 && (
            <div className="slot-info-field">
              <label>Type de créneau</label>
              <select value={infoSlotType} onChange={e => setInfoSlotType(e.target.value)}>
                <option value="">— Aucun —</option>
                {(config?.slotTypes ?? []).map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          )}
          {(config?.clubs ?? []).length > 0 && (
            <div className="slot-info-field">
              <label>Club</label>
              <select value={infoClub} onChange={e => setInfoClub(e.target.value)}>
                <option value="">— Aucun —</option>
                {(config?.clubs ?? []).map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
          )}
          <div className="slot-info-field">
            <label>Titre</label>
            <input type="text" value={infoTitle} onChange={e => setInfoTitle(e.target.value)} placeholder="Ex: Formation niveau 1" />
          </div>
          <div className="slot-info-field">
            <label>Notes</label>
            <textarea value={infoNotes} onChange={e => setInfoNotes(e.target.value)} rows={2} placeholder="Informations supplémentaires..." />
          </div>
          <div className="diver-form-actions" style={{ marginTop: 6 }}>
            <button type="submit" disabled={infoSaving} className="btn-diver-save">{infoSaving ? '...' : '✓ Enregistrer'}</button>
            <button type="button" className="btn-diver-cancel" onClick={() => { setEditingInfo(false); setInfoError(''); }}>Annuler</button>
          </div>
        </form>
      ) : (
        <>
          {currentNotes && <div className="slot-tooltip-notes">📝 {currentNotes}</div>}
          {canEditThisSlot && !editingDiverCount && (
            <button className="btn-edit-slot-info" onClick={startEditInfo}>✏️ Modifier le créneau</button>
          )}
        </>
      )}

      {/* Liste des plongeurs */}
      <div className="slot-tooltip-divers">
        <div className="slot-tooltip-divers-title">
          Plongeurs inscrits ({usedDivers}/{currentDiverCount})
        </div>

        {loading
          ? <p className="slot-tooltip-empty">Chargement...</p>
          : divers.length === 0
            ? <p className="slot-tooltip-empty">Aucun plongeur inscrit</p>
            : (
              <ul className="diver-list">
                {divers.map(d => (
                  <li key={d.id} className={`diver-item ${d.isDirector ? 'diver-item-director' : ''}`}>
                    {/* Mode affichage */}
                    {editingDiver?.id !== d.id ? (
                      <>
                        <div className="diver-main-row">
                          {d.isDirector && <span className="diver-director-badge" title="Directeur de plongée">🎖</span>}
                          <span className="diver-level-dot" style={{ background: getLevelColor(d.level) }} title={d.level} />
                          <span className="diver-name">{d.firstName} {d.lastName}</span>
                          {canEditThisSlot && (
                            <div className="diver-actions">
                              <button className="diver-edit" onClick={() => startEdit(d)} title="Modifier">✏️</button>
                              <button className="diver-remove" onClick={() => handleRemoveDiver(d.id)} title="Retirer">✕</button>
                            </div>
                          )}
                        </div>
                        <div className="diver-sub-row">
                          {d.isDirector && <span className="diver-function-tag">Dir. plongée</span>}
                          <span className="diver-level">{d.level}</span>
                        </div>
                        {d.isDirector && (d.email || d.phone) && (
                          <div className="diver-director-coords">
                            {d.email && <a href={`mailto:${d.email}`} className="diver-coord-link">📧 {d.email}</a>}
                            {d.phone && <a href={`tel:${d.phone}`} className="diver-coord-link">📞 {d.phone}</a>}
                          </div>
                        )}
                      </>
                    ) : (
                      /* Mode édition inline */
                      <form onSubmit={handleUpdateDiver} className="diver-edit-form">
                        {editError && <div className="diver-form-error">{editError}</div>}

                        {/* Recherche utilisateur existant */}
                        <div className="user-search-wrapper">
                          <input className="user-search-input"
                            placeholder="🔍 Rechercher un utilisateur..."
                            value={editSearchQuery}
                            onChange={e => setEditSearchQuery(e.target.value)}
                            autoComplete="off" />
                          {editSearchLoading && <div className="user-search-hint">Recherche...</div>}
                          {!editSearchLoading && editSearchResults.length > 0 && (
                            <ul className="user-search-results">
                              {editSearchResults.map(u => (
                                <li key={u.id} className="user-search-item" onMouseDown={() => selectUserForEdit(u)}>
                                  <span className="user-search-name">{u.name}</span>
                                  <span className="user-search-email">{u.email}</span>
                                </li>
                              ))}
                            </ul>
                          )}
                          {!editSearchLoading && editSearchQuery.trim() && editSearchResults.length === 0 && (
                            <div className="user-search-hint">Aucun utilisateur trouvé</div>
                          )}
                        </div>

                        <div className="diver-form-row">
                          <input placeholder="Prénom *" value={editForm.firstName} required
                            onChange={e => setEditForm(f => ({ ...f, firstName: e.target.value }))} />
                          <input placeholder="Nom *" value={editForm.lastName} required
                            onChange={e => setEditForm(f => ({ ...f, lastName: e.target.value }))} />
                        </div>
                        <select value={editForm.level}
                          onChange={e => setEditForm(f => ({ ...f, level: e.target.value }))}>
                          {(config?.levels ?? []).map(l => <option key={l} value={l}>{l}</option>)}
                        </select>

                        {/* Case directeur indépendante du niveau */}
                        <div className="diver-director-toggle-row">
                          {(!hasDirector || d.isDirector) ? (
                            <label className="diver-director-checkbox">
                              <input type="checkbox" checked={editForm.isDirector}
                                onChange={e => setEditForm(f => ({ ...f, isDirector: e.target.checked }))} />
                              <span>🎖 Directeur de plongée sur ce créneau</span>
                            </label>
                          ) : (
                            <div className="user-search-hint" style={{ color: '#92400e' }}>⚠️ Un directeur est déjà inscrit</div>
                          )}
                        </div>

                        {editForm.isDirector && (
                          <div className="diver-director-fields">
                            <div className="diver-director-fields-label">📋 Coordonnées (obligatoires)</div>
                            <input type="email" placeholder="Email *" required
                              value={editForm.email ?? ''}
                              onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} />
                            <input type="tel" placeholder="Téléphone *" required
                              value={editForm.phone ?? ''}
                              onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} />
                          </div>
                        )}
                        <div className="diver-form-actions">
                          <button type="submit" disabled={editSaving} className="btn-diver-save">
                            {editSaving ? '...' : '✓ Enregistrer'}
                          </button>
                          <button type="button" className="btn-diver-cancel"
                            onClick={() => { setEditingDiver(null); setEditError(''); }}>
                            Annuler
                          </button>
                        </div>
                      </form>
                    )}
                  </li>
                ))}
              </ul>
            )
        }
      </div>

      {/* Formulaire d'ajout d'un plongeur */}
      {canEditThisSlot && usedDivers < currentDiverCount && (
        <div className="slot-tooltip-add">
          {!showDiverForm ? (
            <button
              className="btn-add-diver"
              onClick={() => { setShowDiverForm(true); setError(''); setForm(EMPTY_FORM); setSearchQuery(''); setSearchResults([]); }}
            >
              + Ajouter un plongeur
            </button>
          ) : (
            <form onSubmit={handleAddDiver} className="diver-form">
              {error && <div className="diver-form-error">{error}</div>}
              {addSuccess && <div className="diver-form-success">{addSuccess}</div>}

              {/* Recherche utilisateur existant */}
              <div className="user-search-wrapper">
                <input
                  className="user-search-input"
                  placeholder="🔍 Rechercher un utilisateur..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  autoComplete="off"
                />
                {searchLoading && <div className="user-search-hint">Recherche...</div>}
                {!searchLoading && searchResults.length > 0 && (
                  <ul className="user-search-results">
                    {searchResults.map(u => (
                      <li key={u.id} className="user-search-item" onMouseDown={() => selectUserForAdd(u)}>
                        <span className="user-search-name">{u.name}</span>
                        <span className="user-search-email">{u.email}</span>
                      </li>
                    ))}
                  </ul>
                )}
                {!searchLoading && searchQuery.trim() && searchResults.length === 0 && (
                  <div className="user-search-hint">Aucun utilisateur trouvé</div>
                )}
              </div>

              <div className="diver-form-row">
                <input placeholder="Prénom *" value={form.firstName} required
                  onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} />
                <input placeholder="Nom *" value={form.lastName} required
                  onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} />
              </div>
              <select value={form.level} onChange={e => setForm(f => ({ ...f, level: e.target.value }))}>
                {(config?.levels ?? []).map(l => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>

              {/* Case à cocher directeur — indépendante du niveau */}
              <div className="diver-director-toggle-row">
                {!hasDirector ? (
                  <label className="diver-director-checkbox">
                    <input type="checkbox" checked={form.isDirector}
                      onChange={e => setForm(f => ({ ...f, isDirector: e.target.checked }))} />
                    <span>🎖 Directeur de plongée sur ce créneau</span>
                  </label>
                ) : (
                  <div className="user-search-hint" style={{ color: '#92400e' }}>⚠️ Un directeur est déjà inscrit</div>
                )}
              </div>

              {/* Champs email/téléphone obligatoires si directeur */}
              {form.isDirector && (
                <div className="diver-director-fields">
                  <div className="diver-director-fields-label">📋 Coordonnées du directeur (obligatoires)</div>
                  <input type="email" placeholder="Email *" value={form.email ?? ''} required
                    onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
                  <input type="tel" placeholder="Téléphone *" value={form.phone ?? ''} required
                    onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
                </div>
              )}
              <div className="diver-form-actions">
                <button type="submit" disabled={saving} className="btn-diver-save">{saving ? '...' : 'Ajouter'}</button>
                <button type="button" className="btn-diver-cancel"
                  onClick={() => { setShowDiverForm(false); setError(''); setSearchQuery(''); setSearchResults([]); resetIOSZoom(); }}>
                  Annuler
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* Bouton export fiche de sécurité */}
      {canEditThisSlot && (
        <button className="btn-export-fiche"
          onClick={() => exportFicheSecurite(
            { ...slot, title: currentTitle, notes: currentNotes, slotType: currentSlotType, club: currentClub, diverCount: currentDiverCount },
            divers
          ).catch(err => console.error('Export fiche sécurité :', err))}>
          📊 Exporter fiche de sécurité (Excel)
        </button>
      )}

      {/* Bouton supprimer le créneau */}
      {canEditThisSlot && (
        <button className="btn-delete-slot-tooltip"
          onClick={() => { closeTooltip(); onDelete(slot.id); }}>
          🗑 Supprimer le créneau
        </button>
      )}
    </div>,
    document.body   // ← Portal : rendu directement dans <body>, hors de tout overflow
  ) : null;

  return (
    <>
      <div
        ref={blockRef}
        className={`slot-block ${isCompact ? 'compact' : ''} ${showTooltip ? 'slot-block-active' : ''}`}
        style={{
          borderLeftColor: getSlotTypeStyle(currentSlotType).border,
          background: getSlotTypeStyle(currentSlotType).bg,
          cursor: 'pointer',
        }}
        onClick={handleBlockClick}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
        <div className="slot-block-content">
          <div className="slot-block-time" style={{ color: getSlotTypeStyle(currentSlotType).color }}>{slot.startTime} – {slot.endTime}</div>
          {!isCompact && currentSlotType && (
            <div className="slot-block-type" style={{ background: getSlotTypeStyle(currentSlotType).tagBg, color: getSlotTypeStyle(currentSlotType).tagColor }}>
              {currentSlotType}
            </div>
          )}
          {!isCompact && currentClub     && <div className="slot-block-club">🏊 {currentClub}</div>}
          {!isCompact && currentTitle    && <div className="slot-block-title">{currentTitle}</div>}
          <div className="slot-block-count" style={{ color }}>🤿 {usedDivers}/{currentDiverCount}</div>
          {canEditThisSlot && !isCompact && (
            <button className="slot-block-delete"
              onClick={e => { e.stopPropagation(); onDelete(slot.id); }}
              title="Supprimer">✕</button>
          )}
        </div>
        <div className="slot-block-bar">
          <div className="slot-block-bar-fill"
            style={{ width: `${Math.min(100, (usedDivers / currentDiverCount) * 100)}%`, background: color }} />
        </div>
      </div>
      {hoverTooltipContent}
      {tooltipContent}
    </>
  );
}
