import { useEffect, useState, useCallback } from 'react';
import dayjs from 'dayjs';
import type { DiveSlot } from '../types';
import { slotService } from '../services/slotService';
import { useAuth } from '../context/AuthContext';

interface Props {
  year: number;
  month: number;        // 1-12
  onSelectDay: (date: string) => void;
  onAdd: (date: string) => void;
}

const DOW_FR = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

function getCapacityColor(registered: number, capacity: number): string {
  if (capacity === 0) return '#6b7280';
  const pct = registered / capacity;
  if (pct >= 1)   return '#ef4444';
  if (pct >= 0.8) return '#f59e0b';
  if (pct > 0)    return '#10b981';
  return '#6b7280';
}

export function MonthView({ year, month, onSelectDay, onAdd }: Props) {
  const { user, isAuthenticated } = useAuth();
  const [slots, setSlots]         = useState<DiveSlot[]>([]);
  const [loading, setLoading]     = useState(true);
  const canEdit = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');

  const today = dayjs().format('YYYY-MM-DD');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await slotService.getByMonth(year, month);
      setSlots(data);
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  useEffect(() => { load(); }, [load]);

  // Grouper les créneaux par date
  const slotsByDate = slots.reduce<Record<string, DiveSlot[]>>((acc, s) => {
    if (!acc[s.slotDate]) acc[s.slotDate] = [];
    acc[s.slotDate].push(s);
    return acc;
  }, {});

  // Construire la grille du mois (lundi = premier jour)
  const firstDay  = dayjs(`${year}-${String(month).padStart(2, '0')}-01`);
  const daysInMonth = firstDay.daysInMonth();
  // Décalage : 0=lun … 6=dim (ISO)
  const startOffset = (firstDay.day() + 6) % 7;
  // Nombre total de cellules (multiple de 7)
  const totalCells = Math.ceil((daysInMonth + startOffset) / 7) * 7;
  const cells: (number | null)[] = [
    ...Array(startOffset).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
    ...Array(totalCells - daysInMonth - startOffset).fill(null),
  ];

  const monthLabel = firstDay.locale('fr').format('MMMM YYYY');

  return (
    <div className="month-view">
      <div className="month-title">{monthLabel.charAt(0).toUpperCase() + monthLabel.slice(1)}</div>

      {loading ? (
        <div className="loading">Chargement...</div>
      ) : (
        <div className="month-grid">
          {/* En-têtes des jours */}
          {DOW_FR.map(d => (
            <div key={d} className="month-dow">{d}</div>
          ))}

          {/* Cellules */}
          {cells.map((dayNum, idx) => {
            if (dayNum === null) {
              return <div key={`empty-${idx}`} className="month-cell month-cell-empty" />;
            }
            const dateStr  = `${year}-${String(month).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
            const daySlots = slotsByDate[dateStr] ?? [];
            const isToday  = dateStr === today;
            const totalRegistered = daySlots.reduce((a, s) => a + (s.divers?.length ?? 0), 0);
            const totalCapacity   = daySlots.reduce((a, s) => a + s.diverCount, 0);
            const dotColor = daySlots.length > 0 ? getCapacityColor(totalRegistered, totalCapacity) : '';

            return (
              <div
                key={dateStr}
                className={`month-cell ${isToday ? 'month-cell-today' : ''}`}
                onClick={() => onSelectDay(dateStr)}
              >
                {/* Numéro du jour */}
                <div className={`month-day-num ${isToday ? 'month-day-num-today' : ''}`}>
                  {dayNum}
                </div>

                {/* Pastille + compteur de créneaux */}
                {daySlots.length > 0 && (
                  <div className="month-slots-summary">
                    <span className="month-slots-dot" style={{ background: dotColor }} />
                    <span className="month-slots-count">
                      {daySlots.length} créneau{daySlots.length > 1 ? 'x' : ''}
                    </span>
                    <span className="month-slots-divers" style={{ color: dotColor }}>
                      🤿 {totalRegistered}/{totalCapacity}
                    </span>
                  </div>
                )}

                {/* Liste des premiers créneaux (max 3) */}
                <div className="month-slot-pills">
                  {daySlots.slice(0, 3).map(s => (
                    <div
                      key={s.id}
                      className="month-slot-pill"
                      title={`${s.startTime}–${s.endTime}${s.title ? ' · ' + s.title : ''}`}
                      style={{
                        background: getCapacityColor(s.divers?.length ?? 0, s.diverCount) + '22',
                        borderLeft: `3px solid ${getCapacityColor(s.divers?.length ?? 0, s.diverCount)}`,
                      }}
                    >
                      <span className="month-pill-time">{s.startTime}–{s.endTime}</span>
                      {s.title && <span className="month-pill-title">{s.title}</span>}
                    </div>
                  ))}
                  {daySlots.length > 3 && (
                    <div className="month-slot-pill-more">+{daySlots.length - 3} autre{daySlots.length - 3 > 1 ? 's' : ''}</div>
                  )}
                </div>

                {/* Bouton + pour admin/directeur */}
                {canEdit && (
                  <button
                    className="month-cell-add"
                    onClick={e => { e.stopPropagation(); onAdd(dateStr); }}
                    title="Ajouter un créneau"
                  >+</button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

