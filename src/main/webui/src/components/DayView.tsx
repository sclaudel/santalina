import { useEffect, useState, useCallback } from 'react';
import type { DiveSlot, AppConfig } from '../types';
import { slotService } from '../services/slotService';
import { useAuth } from '../context/AuthContext';
import { TimeGrid } from './TimeGrid';

interface Props {
  date: string;
  config: AppConfig;
  onAdd: (date: string, startTime?: string) => void;
  onOpenPalanquees?: (slotId: number) => void;
  autoExpandSlotId?: number;
}

export function DayView({ date, config, onAdd, onOpenPalanquees, autoExpandSlotId }: Props) {
  const { user, isAuthenticated } = useAuth();
  const [slots, setSlots]       = useState<DiveSlot[]>([]);
  const [loading, setLoading]   = useState(true);

  const canEdit = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const data = await slotService.getByDate(date);
      setSlots(data);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [date]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: number) => {
    if (!confirm('Supprimer ce créneau ?')) return;
    await slotService.delete(id);
    load();
  };

  const label = new Date(date + 'T00:00:00').toLocaleDateString('fr-FR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  });

  const totalRegistered = slots.reduce((acc, s) => acc + (s.divers?.length ?? 0), 0);
  const totalCapacity   = slots.reduce((acc, s) => acc + s.diverCount, 0);

  return (
    <div className="day-view">
      <div className="day-view-header">
        <h2 style={{ textTransform: 'capitalize' }}>{label}</h2>
        <span className="day-stats">
          {slots.length} créneau{slots.length !== 1 ? 'x' : ''} · {totalRegistered}/{totalCapacity} plongeurs inscrits
        </span>
      </div>

      {loading ? (
        <div className="loading">Chargement...</div>
      ) : slots.length === 0 ? (
        <div className="empty-state">
          <p>🌊 Aucun créneau ce jour</p>
          {canEdit && (
            <button className="btn btn-primary" onClick={() => onAdd(date)}>
              + Créer le premier créneau
            </button>
          )}
        </div>
      ) : (
        <TimeGrid
          slots={slots}
          config={config}
          onDelete={handleDelete}
          onRefresh={() => load(true)}
          canEdit={canEdit}
          currentUserId={user?.id}
          currentUserRole={user?.role}
          onClickTime={canEdit ? (time) => onAdd(date, time) : undefined}
          onOpenPalanquees={onOpenPalanquees}
          autoExpandSlotId={autoExpandSlotId}
        />
      )}
    </div>
  );
}
