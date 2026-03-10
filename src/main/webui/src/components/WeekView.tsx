import { useEffect, useState, useCallback, useMemo } from 'react';
import dayjs from 'dayjs';
import type { DiveSlot, AppConfig } from '../types';
import { slotService } from '../services/slotService';
import { useAuth } from '../context/AuthContext';
import { TimeGrid } from './TimeGrid';

interface Props {
  weekStart: string;
  config: AppConfig;
  onSelectDay: (date: string) => void;
  onAdd: (date: string) => void;
}

const DAYS_FR = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

export function WeekView({ weekStart, config, onSelectDay, onAdd }: Props) {
  const { user, isAuthenticated } = useAuth();
  const [allSlots, setAllSlots] = useState<DiveSlot[]>([]);
  const [loading, setLoading]   = useState(true);

  const canEdit = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');
  const today   = dayjs().format('YYYY-MM-DD');

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const data = await slotService.getByWeek(weekStart);
      setAllSlots(data);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [weekStart]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: number) => {
    if (!confirm('Supprimer ce créneau ?')) return;
    await slotService.delete(id);
    load();
  };

  const days = Array.from({ length: 7 }, (_, i) =>
    dayjs(weekStart).add(i, 'day').format('YYYY-MM-DD')
  );

  // Plage horaire dynamique : 1h de marge avant le premier début et après la dernière fin
  const { startHour, endHour } = useMemo(() => {
    if (!allSlots.length) return { startHour: 6, endHour: 22 };
    const toMin = (t: string) => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
    const minStart = Math.min(...allSlots.map(s => toMin(s.startTime)));
    const maxEnd   = Math.max(...allSlots.map(s => toMin(s.endTime)));
    return {
      startHour: Math.max(0,  Math.floor(minStart / 60) - 1),
      endHour:   Math.min(24, Math.ceil(maxEnd   / 60) + 1),
    };
  }, [allSlots]);

  return (
    <div className="week-view-chrono">
      {/* En-tête des 7 jours */}
      <div className="week-header-row">
        <div className="week-time-spacer" />
        {days.map((d, i) => {
          const isToday = d === today;
          const dayNum  = dayjs(d).date();
          const count   = allSlots.filter(s => s.slotDate === d).length;
          return (
            <div key={d} className={`week-day-col-header ${isToday ? 'today' : ''}`}
              onClick={() => onSelectDay(d)}>
              <span className="week-col-dayname">{DAYS_FR[i]}</span>
              <span className={`week-col-daynum ${isToday ? 'today-num' : ''}`}>{dayNum}</span>
              {count > 0 && <span className="week-col-count">{count} créneau{count > 1 ? 'x' : ''}</span>}
              {canEdit && (
                <button className="week-col-add"
                  onClick={e => { e.stopPropagation(); onAdd(d); }}
                  title={`Ajouter un créneau le ${d}`}>+</button>
              )}
            </div>
          );
        })}
      </div>

      {loading ? (
        <div className="loading">Chargement...</div>
      ) : (
        <div className="week-grid-chrono">
          <div className="week-time-axis">
            {Array.from({ length: endHour - startHour + 1 }, (_, i) => startHour + i).map(h => (
              <div key={h} className="week-time-label" style={{ top: (h - startHour) * 60 * 2 + 'px' }}>
                {h === 24 ? '00:00' : `${String(h).padStart(2, '0')}:00`}
              </div>
            ))}
          </div>
          {days.map(d => (
            <div key={d} className="week-day-column">
              <TimeGrid
                slots={allSlots.filter(s => s.slotDate === d)}
                config={config}
                onDelete={handleDelete}
                onRefresh={() => load(true)}
                canEdit={canEdit}
                currentUserId={user?.id}
                currentUserRole={user?.role}
                startHour={startHour}
                endHour={endHour}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
