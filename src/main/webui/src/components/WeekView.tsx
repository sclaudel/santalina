import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import type { DiveSlot, AppConfig } from '../types';
import { slotService } from '../services/slotService';
import { SlotCard } from './SlotCard';
import { SlotForm } from './SlotForm';
import { useAuth } from '../context/AuthContext';

dayjs.extend(isoWeek);

interface Props {
  weekStart: string; // YYYY-MM-DD (lundi)
  config: AppConfig;
  onSelectDay: (date: string) => void;
}

export function WeekView({ weekStart, config, onSelectDay }: Props) {
  const [slots, setSlots] = useState<DiveSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [showFormFor, setShowFormFor] = useState<string | null>(null);
  const { isAuthenticated, user } = useAuth();

  const canCreate = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');

  const days = Array.from({ length: 7 }, (_, i) => dayjs(weekStart).add(i, 'day'));

  const load = async () => {
    setLoading(true);
    try {
      const data = await slotService.getByWeek(weekStart);
      setSlots(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [weekStart]);

  const handleDelete = async (id: number) => {
    if (!confirm('Supprimer ce créneau ?')) return;
    await slotService.delete(id);
    load();
  };

  const slotsForDay = (date: string) =>
    slots.filter(s => s.slotDate === date).sort((a, b) => a.startTime.localeCompare(b.startTime));

  return (
    <div className="week-view">
      {loading && <div className="loading">Chargement...</div>}
      <div className="week-grid">
        {days.map(day => {
          const dateStr = day.format('YYYY-MM-DD');
          const daySlots = slotsForDay(dateStr);
          const totalDivers = daySlots.reduce((s, sl) => s + sl.diverCount, 0);
          const isToday = dateStr === dayjs().format('YYYY-MM-DD');

          return (
            <div key={dateStr} className={`week-day ${isToday ? 'today' : ''}`}>
              <div className="week-day-header" onClick={() => onSelectDay(dateStr)}>
                <span className="week-day-name">{day.format('ddd')}</span>
                <span className={`week-day-num ${isToday ? 'today-num' : ''}`}>{day.date()}</span>
                {totalDivers > 0 && (
                  <span className="week-day-count">🤿 {totalDivers}</span>
                )}
              </div>
              <div className="week-slots">
                {daySlots.map(slot => (
                  <SlotCard
                    key={slot.id}
                    slot={slot}
                    maxDivers={config.maxDivers}
                    onDelete={handleDelete}
                  />
                ))}
                {canCreate && (
                  <button className="btn-add-day" onClick={() => setShowFormFor(dateStr)}>+</button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {showFormFor && (
        <SlotForm
          date={showFormFor}
          config={config}
          onCreated={() => { setShowFormFor(null); load(); }}
          onCancel={() => setShowFormFor(null)}
        />
      )}
    </div>
  );
}

