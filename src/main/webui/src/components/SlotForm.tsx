import { useState } from 'react';
import type { AppConfig, SlotRequest } from '../types';
import { slotService } from '../services/slotService';

interface Props {
  date: string;
  config: AppConfig;
  onCreated: () => void;
  onCancel: () => void;
}

function timeOptions(resolutionMinutes: number): string[] {
  const resolution = (resolutionMinutes > 0 && isFinite(resolutionMinutes)) ? resolutionMinutes : 15;
  const opts: string[] = [];
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += resolution) {
      opts.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
    }
  }
  return opts;
}

export function SlotForm({ date, config, onCreated, onCancel }: Props) {
  // Valeurs de sécurité si config pas encore chargée
  const safeConfig = {
    maxDivers: config?.maxDivers > 0 ? config.maxDivers : 25,
    slotResolutionMinutes: config?.slotResolutionMinutes > 0 ? config.slotResolutionMinutes : 15,
    slotMinHours: config?.slotMinHours > 0 ? config.slotMinHours : 1,
    slotMaxHours: config?.slotMaxHours > 0 ? config.slotMaxHours : 10,
  };
  const times = timeOptions(safeConfig.slotResolutionMinutes);

  const [startTime, setStartTime] = useState('08:00');
  const [endTime, setEndTime] = useState('10:00');
  const [diverCount, setDiverCount] = useState(1);
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const req: SlotRequest = { slotDate: date, startTime, endTime, diverCount, title, notes };
      await slotService.create(req);
      onCreated();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || 'Erreur lors de la création du créneau');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="slot-form-overlay" onClick={onCancel}>
      <div className="slot-form" onClick={e => e.stopPropagation()}>
        <h3>➕ Nouveau créneau — {date}</h3>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label>Heure de début</label>
              <select value={startTime} onChange={e => setStartTime(e.target.value)}>
                {times.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Heure de fin</label>
              <select value={endTime} onChange={e => setEndTime(e.target.value)}>
                {times.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Nombre de plongeurs (max {safeConfig.maxDivers})</label>
            <input
              type="number" min={1} max={safeConfig.maxDivers}
              value={diverCount}
              onChange={e => setDiverCount(Number(e.target.value))}
              required
            />
          </div>
          <div className="form-group">
            <label>Titre (optionnel)</label>
            <input type="text" value={title} onChange={e => setTitle(e.target.value)} placeholder="Ex: Formation niveau 1" />
          </div>
          <div className="form-group">
            <label>Notes (optionnel)</label>
            <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} placeholder="Informations supplémentaires..." />
          </div>
          <div className="form-actions">
            <button type="button" className="btn btn-outline" onClick={onCancel}>Annuler</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Création...' : 'Créer le créneau'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

