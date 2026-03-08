import { useEffect, useState, useCallback } from 'react';
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
            {Array.from({ length: 17 }, (_, i) => i + 6).map(h => (
              <div key={h} className="week-time-label" style={{ top: (h - 6) * 60 * 2 + 'px' }}>
                {String(h).padStart(2, '0')}:00
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
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
