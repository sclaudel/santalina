import type { DiveSlot } from '../types';
import { useAuth } from '../context/AuthContext';

interface Props {
  slot: DiveSlot;
  maxDivers: number;
  onDelete?: (id: number) => void;
}

export function SlotCard({ slot, maxDivers, onDelete }: Props) {
  const { user, isAuthenticated } = useAuth();

  const canDelete =
    isAuthenticated &&
    (user?.role === 'ADMIN' ||
      (user?.role === 'DIVE_DIRECTOR' && slot.createdById === user?.id));

  const fillPercent = Math.min(100, (slot.diverCount / maxDivers) * 100);
  const fillColor =
    fillPercent >= 90 ? '#ef4444' :
    fillPercent >= 70 ? '#f59e0b' : '#22c55e';

  return (
    <div className="slot-card">
      <div className="slot-card-header">
        <div className="slot-time">
          ⏰ {slot.startTime} – {slot.endTime}
        </div>
        {canDelete && (
          <button
            className="btn-icon btn-delete"
            title="Supprimer ce créneau"
            onClick={() => onDelete?.(slot.id)}
          >
            🗑️
          </button>
        )}
      </div>

      {slot.title && <div className="slot-title">{slot.title}</div>}

      <div className="slot-divers">
        <div className="slot-divers-count">
          🤿 <strong>{slot.diverCount}</strong> plongeur{slot.diverCount > 1 ? 's' : ''}
        </div>
        <div className="slot-progress-bar">
          <div
            className="slot-progress-fill"
            style={{ width: `${fillPercent}%`, backgroundColor: fillColor }}
          />
        </div>
        <div className="slot-diver-max">max {maxDivers}</div>
      </div>

      <div className="slot-director">
        👤 {slot.createdByName}
      </div>

      {slot.notes && (
        <div className="slot-notes">📝 {slot.notes}</div>
      )}
    </div>
  );
}

