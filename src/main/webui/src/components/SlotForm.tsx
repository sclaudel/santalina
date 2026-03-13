import { useState } from 'react';
import type { AppConfig, SlotRequest } from '../types';
import { slotService } from '../services/slotService';

interface Props {
  date: string;
  config: AppConfig;
  onCreated: () => void;
  onCancel: () => void;
  /** Heure de début pré-remplie (ex: issu d'un clic sur la grille) */
  initialStartTime?: string;
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

function toMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

export function SlotForm({ date, config, onCreated, onCancel, initialStartTime }: Props) {
  const safeConfig = {
    maxDivers: config?.maxDivers > 0 ? config.maxDivers : 25,
    slotResolutionMinutes: config?.slotResolutionMinutes > 0 ? config.slotResolutionMinutes : 15,
    slotMinHours: config?.slotMinHours > 0 ? config.slotMinHours : 1,
    slotMaxHours: config?.slotMaxHours > 0 ? config.slotMaxHours : 10,
    defaultSlotHours: config?.defaultSlotHours > 0 ? config.defaultSlotHours : 2,
    slotTypes: config?.slotTypes ?? [],
    clubs: config?.clubs ?? [],
    bookingOpenHour: config?.bookingOpenHour ?? -1,
    bookingCloseHour: config?.bookingCloseHour ?? -1,
  };
  const times = timeOptions(safeConfig.slotResolutionMinutes);

  const [slotDate, setSlotDate]           = useState(date);
  const [startTime, setStartTime]         = useState(initialStartTime ?? '08:00');
  const [endTime, setEndTime]         = useState(() => {
    const base = initialStartTime ?? '08:00';
    const startMin = toMinutes(base);
    const defaultMin = safeConfig.defaultSlotHours * 60;
    const valid = times.find(t => toMinutes(t) - startMin === defaultMin)
      ?? times.find(t => {
        const d = toMinutes(t) - startMin;
        return d >= safeConfig.slotMinHours * 60 && d <= safeConfig.slotMaxHours * 60;
      });
    return valid ?? '10:00';
  });
  const [diverCountStr, setDiverCountStr] = useState('2');
  const [title, setTitle]             = useState('');
  const [notes, setNotes]             = useState('');
  const [slotType, setSlotType]       = useState(safeConfig.slotTypes[0] ?? '');
  const [club, setClub]               = useState(safeConfig.clubs[0] ?? '');
  const [error, setError]             = useState('');
  const [loading, setLoading]         = useState(false);

  const handleStartTimeChange = (newStart: string) => {
    setStartTime(newStart);
    const startMin = toMinutes(newStart);
    const diff = toMinutes(endTime) - startMin;
    const minDiff = safeConfig.slotMinHours * 60;
    const maxDiff = safeConfig.slotMaxHours * 60;
    if (diff < minDiff || diff > maxDiff) {
      const valid = times.find(t => {
        const d = toMinutes(t) - startMin;
        return d >= minDiff && d <= maxDiff;
      });
      if (valid) setEndTime(valid);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const slotDateVal = new Date(slotDate + 'T00:00:00');
    if (slotDateVal < today) {
      setError('Impossible de créer un créneau dans le passé');
      return;
    }
    const diverCount = parseInt(diverCountStr, 10);
    if (!diverCountStr || isNaN(diverCount) || diverCount < 2) {
      setError('Le nombre de plongeurs doit être au moins 2');
      return;
    }
    if (diverCount > safeConfig.maxDivers) {
      setError(`Le nombre de plongeurs ne peut pas dépasser ${safeConfig.maxDivers}`);
      return;
    }
    setLoading(true);
    try {
      const req: SlotRequest = {
        slotDate: slotDate, startTime, endTime, diverCount, title, notes,
        slotType: slotType || undefined,
        club: club || undefined,
      };
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
        <h3>➕ Nouveau créneau</h3>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Date</label>
            <input
              type="date"
              value={slotDate}
              onChange={e => setSlotDate(e.target.value)}
              required
            />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Heure de début</label>
              <select value={startTime} onChange={e => handleStartTimeChange(e.target.value)}>
                {times.map(t => {
                  const h = parseInt(t.split(':')[0], 10);
                  const forbidden =
                    (safeConfig.bookingOpenHour  !== -1 && h <  safeConfig.bookingOpenHour) ||
                    (safeConfig.bookingCloseHour !== -1 && h >= safeConfig.bookingCloseHour);
                  return (
                    <option key={t} value={t} disabled={forbidden}
                      style={forbidden ? { color: '#9ca3af' } : {}}>
                      {t}
                    </option>
                  );
                })}
              </select>
            </div>
            <div className="form-group">
              <label>Heure de fin</label>
              <select value={endTime} onChange={e => setEndTime(e.target.value)}>
                {times.map(t => {
                  const diff = toMinutes(t) - toMinutes(startTime);
                  const disabled =
                    diff < safeConfig.slotMinHours * 60 ||
                    diff > safeConfig.slotMaxHours * 60;
                  return (
                    <option key={t} value={t} disabled={disabled}
                      style={disabled ? { color: '#9ca3af' } : {}}>
                      {t}
                    </option>
                  );
                })}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Nombre de plongeurs (max {safeConfig.maxDivers})</label>
            <input
              type="number" min={2} max={safeConfig.maxDivers}
              value={diverCountStr}
              onChange={e => setDiverCountStr(e.target.value)}
              onBlur={() => {
                const val = parseInt(diverCountStr, 10);
                if (!diverCountStr || isNaN(val) || val < 2) setDiverCountStr('2');
                else if (val > safeConfig.maxDivers) setDiverCountStr(String(safeConfig.maxDivers));
              }}
              required
            />
          </div>
          {safeConfig.slotTypes.length > 0 && (
            <div className="form-group">
              <label>Type de créneau</label>
              <select value={slotType} onChange={e => setSlotType(e.target.value)}>
                <option value="">— Aucun —</option>
                {safeConfig.slotTypes.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          )}
          {safeConfig.clubs.length > 0 && (
            <div className="form-group">
              <label>Club</label>
              <select value={club} onChange={e => setClub(e.target.value)}>
                <option value="">— Aucun —</option>
                {safeConfig.clubs.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
          )}
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
