import { useMemo, useRef } from 'react';
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
}

const START_HOUR = 6;   // 06:00
const END_HOUR   = 22;  // 22:00
const PX_PER_MIN = 2;   // 2px par minute → 1h = 120px

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

export function TimeGrid({ slots, config, onDelete, onRefresh, canEdit, currentUserId, currentUserRole }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const totalMinutes = (END_HOUR - START_HOUR) * 60;
  const totalHeight  = totalMinutes * PX_PER_MIN;
  const hours = Array.from({ length: END_HOUR - START_HOUR + 1 }, (_, i) => START_HOUR + i);

  const columns = useMemo(() => computeColumns(slots), [slots]);

  return (
    <div className="time-grid-wrapper">
      <div className="time-grid" style={{ height: totalHeight + 'px' }} ref={containerRef}>

        {/* Colonne des heures */}
        <div className="time-axis">
          {hours.map(h => (
            <div
              key={h}
              className="time-axis-label"
              style={{ top: (h - START_HOUR) * 60 * PX_PER_MIN - 8 + 'px' }}
            >
              {String(h).padStart(2, '0')}:00
            </div>
          ))}
        </div>

        {/* Zone des créneaux */}
        <div className="time-grid-slots">
          {/* Lignes horizontales */}
          {hours.map(h => (
            <div
              key={h}
              className={`time-grid-line ${h % 2 === 0 ? 'even' : 'odd'}`}
              style={{ top: (h - START_HOUR) * 60 * PX_PER_MIN + 'px' }}
            />
          ))}
          {/* Demi-heures */}
          {hours.slice(0, -1).map(h => (
            <div
              key={h + 0.5}
              className="time-grid-line half"
              style={{ top: (h - START_HOUR) * 60 * PX_PER_MIN + 30 * PX_PER_MIN + 'px' }}
            />
          ))}

          {/* Créneaux positionnés */}
          {slots.map(slot => {
            const startMin = timeToMinutes(slot.startTime);
            const endMin   = timeToMinutes(slot.endTime);
            const top      = (startMin - START_HOUR * 60) * PX_PER_MIN;
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
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

