import { useMemo, useRef, useState } from 'react';
import type { DiveSlot, AppConfig } from '../types';
import { SlotBlock } from './SlotBlock';

interface Props {
  slots: DiveSlot[];
  config: AppConfig;
  onDelete: (id: number) => void;
  onRefresh: () => void;
  canEdit: boolean;
  currentUserId?: number;
  currentUserRole?: string;
  currentUserEmail?: string;
  /** Heure de début de la grille (fournie par WeekView pour aligner les colonnes) */
  startHour?: number;
  /** Heure de fin de la grille (fournie par WeekView pour aligner les colonnes) */
  endHour?: number;
  /** Callback déclenché quand l'utilisateur clique sur une zone libre de la grille */
  onClickTime?: (time: string) => void;
  onOpenPalanquees?: (slotId: number) => void;
}

const DEFAULT_START = 6;   // 06:00 par défaut
const DEFAULT_END   = 22;  // 22:00 par défaut
const PX_PER_MIN    = 2;   // 2px par minute → 1h = 120px

function timeToMinutes(t: string): number {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}

/** Calcule les colonnes pour les créneaux qui se chevauchent */
function computeColumns(slots: DiveSlot[]): Map<number, { col: number; totalCols: number }> {
  const result = new Map<number, { col: number; totalCols: number }>();
  if (!slots.length) return result;

  // Trier par heure de début
  const sorted = [...slots].sort((a, b) =>
    timeToMinutes(a.startTime) - timeToMinutes(b.startTime));

  // Groupes qui se chevauchent
  const groups: DiveSlot[][] = [];
  let currentGroup: DiveSlot[] = [];
  let maxEnd = 0;

  for (const slot of sorted) {
    const start = timeToMinutes(slot.startTime);
    const end   = timeToMinutes(slot.endTime);
    if (currentGroup.length === 0 || start < maxEnd) {
      currentGroup.push(slot);
      maxEnd = Math.max(maxEnd, end);
    } else {
      groups.push(currentGroup);
      currentGroup = [slot];
      maxEnd = end;
    }
  }
  if (currentGroup.length) groups.push(currentGroup);

  // Assigner les colonnes dans chaque groupe
  for (const group of groups) {
    const cols: number[] = [];
    for (const slot of group) {
      const start = timeToMinutes(slot.startTime);
      const end   = timeToMinutes(slot.endTime);
      // Trouver la première colonne libre
      let col = 0;
      while (cols[col] !== undefined && cols[col] > start) col++;
      cols[col] = end;
      result.set(slot.id, { col, totalCols: group.length });
    }
    // Corriger totalCols = nb colonnes réellement utilisées
    const usedCols = Math.max(...group.map(s => result.get(s.id)!.col)) + 1;
    for (const slot of group) {
      result.set(slot.id, { ...result.get(slot.id)!, totalCols: usedCols });
    }
  }

  return result;
}

export function TimeGrid({ slots, config, onDelete, onRefresh, canEdit, currentUserId, currentUserRole, currentUserEmail, startHour: startHourProp, endHour: endHourProp, onClickTime, onOpenPalanquees }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hoverTime, setHoverTime] = useState<{ time: string; y: number } | null>(null);

  const resolution = config?.slotResolutionMinutes > 0 ? config.slotResolutionMinutes : 15;

  // Calcul dynamique de la plage horaire visible :
  // – si fourni par le parent (WeekView), on l'utilise tel quel pour aligner toutes les colonnes
  // – sinon on calcule depuis les créneaux du jour (vue Jour), avec 1h de marge
  const { startHour, endHour } = useMemo(() => {
    if (startHourProp !== undefined && endHourProp !== undefined) {
      return { startHour: startHourProp, endHour: endHourProp };
    }
    if (!slots.length) {
      return { startHour: startHourProp ?? DEFAULT_START, endHour: endHourProp ?? DEFAULT_END };
    }
    const minStart = Math.min(...slots.map(s => timeToMinutes(s.startTime)));
    const maxEnd   = Math.max(...slots.map(s => timeToMinutes(s.endTime)));
    return {
      startHour: startHourProp ?? Math.max(0, Math.floor(minStart / 60) - 1),
      endHour:   endHourProp   ?? Math.min(24, Math.ceil(maxEnd / 60) + 1),
    };
  }, [slots, startHourProp, endHourProp]);

  const totalMinutes = (endHour - startHour) * 60;
  const totalHeight  = totalMinutes * PX_PER_MIN;
  const hours = Array.from({ length: endHour - startHour + 1 }, (_, i) => startHour + i);

  const columns = useMemo(() => computeColumns(slots), [slots]);

  const computeTimeFromY = (y: number): string => {
    const rawMinutes = y / PX_PER_MIN + startHour * 60;
    const snapped    = Math.round(rawMinutes / resolution) * resolution;
    const clamped    = Math.max(startHour * 60, Math.min(endHour * 60 - resolution, snapped));
    const h = Math.floor(clamped / 60);
    const m = clamped % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  };

  const handleGridMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!onClickTime) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const y = e.clientY - rect.top;
    setHoverTime({ time: computeTimeFromY(y), y });
  };

  const handleGridMouseLeave = () => setHoverTime(null);

  const handleGridClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!onClickTime) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const y = e.clientY - rect.top;
    onClickTime(computeTimeFromY(y));
  };

  return (
    <div className="time-grid-wrapper">
      <div className="time-grid" style={{ height: totalHeight + 'px' }} ref={containerRef}>

        {/* Colonne des heures */}
        <div className="time-axis">
          {hours.map(h => (
            <div
              key={h}
              className="time-axis-label"
              style={{ top: (h - startHour) * 60 * PX_PER_MIN - 8 + 'px' }}
            >
              {h === 24 ? '00:00' : `${String(h).padStart(2, '0')}:00`}
            </div>
          ))}
        </div>

        {/* Zone des créneaux */}
          <div
            className="time-grid-slots"
            style={onClickTime ? { cursor: 'crosshair', position: 'relative' } : { position: 'relative' }}
            onClick={handleGridClick}
            onMouseMove={handleGridMouseMove}
            onMouseLeave={handleGridMouseLeave}
          >
            {/* Indicateur de survol */}
            {onClickTime && hoverTime && (
              <div style={{
                position: 'absolute', left: '50%', transform: 'translateX(-50%)',
                top: hoverTime.y - 11 + 'px',
                background: '#1e40af', color: '#fff',
                fontSize: 11, fontWeight: 700, borderRadius: 4,
                padding: '2px 7px', pointerEvents: 'none', zIndex: 10,
                whiteSpace: 'nowrap', boxShadow: '0 1px 4px rgba(0,0,0,.2)',
              }}>
                + {hoverTime.time}
              </div>
            )}
          {/* Lignes horizontales */}
          {hours.map(h => (
            <div
              key={h}
              className={`time-grid-line ${h % 2 === 0 ? 'even' : 'odd'}`}
              style={{ top: (h - startHour) * 60 * PX_PER_MIN + 'px' }}
            />
          ))}
          {/* Demi-heures */}
          {hours.slice(0, -1).map(h => (
            <div
              key={h + 0.5}
              className="time-grid-line half"
              style={{ top: (h - startHour) * 60 * PX_PER_MIN + 30 * PX_PER_MIN + 'px' }}
            />
          ))}

          {/* Créneaux positionnés */}
          {slots.map(slot => {
            const startMin = timeToMinutes(slot.startTime);
            const endMin   = timeToMinutes(slot.endTime);
            const top      = (startMin - startHour * 60) * PX_PER_MIN;
            const height   = Math.max((endMin - startMin) * PX_PER_MIN, 30);
            const layout   = columns.get(slot.id) ?? { col: 0, totalCols: 1 };
            const widthPct = 100 / layout.totalCols;
            const leftPct  = layout.col * widthPct;

            return (
              <div
                key={slot.id}
                className="time-grid-slot-wrapper"
                style={{
                  top:    top + 'px',
                  height: height + 'px',
                  left:   `calc(${leftPct}% + 2px)`,
                  width:  `calc(${widthPct}% - 4px)`,
                }}
                onClick={e => e.stopPropagation()}
              >
                <SlotBlock
                  slot={slot}
                  maxDivers={config.maxDivers}
                  config={config}
                  height={height}
                  onDelete={onDelete}
                  onRefresh={onRefresh}
                  canEdit={canEdit}
                  currentUserId={currentUserId}
                  currentUserRole={currentUserRole}
                  currentUserEmail={currentUserEmail}
                  onOpenPalanquees={onOpenPalanquees}
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

