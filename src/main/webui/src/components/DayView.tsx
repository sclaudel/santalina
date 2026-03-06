import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import type { DiveSlot, AppConfig } from '../types';
import { slotService } from '../services/slotService';
import { SlotCard } from './SlotCard';
import { SlotForm } from './SlotForm';
import { useAuth } from '../context/AuthContext';

interface Props {
  date: string;
  config: AppConfig;
}

export function DayView({ date, config }: Props) {
  const [slots, setSlots] = useState<DiveSlot[]>([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const { isAuthenticated, user } = useAuth();

  const canCreate = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');

  const load = async () => {
    setLoading(true);
    try {
      const data = await slotService.getByDate(date);
      data.sort((a, b) => a.startTime.localeCompare(b.startTime));
      setSlots(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [date]);

  const handleDelete = async (id: number) => {
    if (!confirm('Supprimer ce créneau ?')) return;
    await slotService.delete(id);
    load();
  };

  const totalDivers = slots.reduce((sum, s) => sum + s.diverCount, 0);

  return (
    <div className="day-view">
      <div className="day-view-header">
        <h2>{dayjs(date).format('dddd D MMMM YYYY')}</h2>
        <div className="day-stats">
          🤿 {totalDivers} plongeur{totalDivers > 1 ? 's' : ''} réservé{totalDivers > 1 ? 's' : ''}
          {' '} / {config.maxDivers} max
        </div>
        {canCreate && (
          <button className="btn btn-primary" onClick={() => setShowForm(true)}>
            ➕ Nouveau créneau
          </button>
        )}
      </div>

      {loading ? (
        <div className="loading">Chargement...</div>
      ) : slots.length === 0 ? (
        <div className="empty-state">
          <p>🌊 Aucun créneau pour cette journée</p>
          {canCreate && (
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>
              Créer le premier créneau
            </button>
          )}
        </div>
      ) : (
        <div className="slots-list">
          {slots.map(slot => (
            <SlotCard
              key={slot.id}
              slot={slot}
              maxDivers={config.maxDivers}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {showForm && (
        <SlotForm
          date={date}
          config={config}
          onCreated={() => { setShowForm(false); load(); }}
          onCancel={() => setShowForm(false)}
        />
      )}
    </div>
  );
}

