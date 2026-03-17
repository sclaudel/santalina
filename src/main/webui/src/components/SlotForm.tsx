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

// Jours de la semaine ISO (1=Lun … 7=Dim)
const DAYS_OF_WEEK = [
  { value: 1, label: 'Lun' },
  { value: 2, label: 'Mar' },
  { value: 3, label: 'Mer' },
  { value: 4, label: 'Jeu' },
  { value: 5, label: 'Ven' },
  { value: 6, label: 'Sam' },
  { value: 7, label: 'Dim' },
];

function addMonths(dateStr: string, months: number): string {
  const d = new Date(dateStr + 'T00:00:00');
  d.setMonth(d.getMonth() + months);
  return d.toISOString().slice(0, 10);
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
    maxRecurringMonths: config?.maxRecurringMonths > 0 ? config.maxRecurringMonths : 4,
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
  const [successMsg, setSuccessMsg]   = useState('');

  // Récurrence
  const [recurring, setRecurring]         = useState(false);
  const [recurringDays, setRecurringDays] = useState<number[]>([]);
  const [recurringUntil, setRecurringUntil] = useState(() => addMonths(date, 1));

  const maxUntilDate = addMonths(slotDate, safeConfig.maxRecurringMonths);

  const toggleDay = (day: number) => {
    setRecurringDays(prev =>
      prev.includes(day) ? prev.filter(d => d !== day) : [...prev, day]
    );
  };

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
    setSuccessMsg('');
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
    if (recurring && recurringDays.length === 0) {
      setError('Sélectionnez au moins un jour pour la récurrence');
      return;
    }
    if (recurring && recurringUntil <= slotDate) {
      setError('La date de fin de récurrence doit être après la date de début');
      return;
    }
    setLoading(true);
    try {
      const req: SlotRequest = {
        slotDate, startTime, endTime, diverCount, title, notes,
        slotType: slotType || undefined,
        club: club || undefined,
        recurring: recurring || undefined,
        recurringDays: recurring ? recurringDays : undefined,
        recurringUntil: recurring ? recurringUntil : undefined,
      };
      const result = await slotService.create(req);
      if (result.created > 1) {
        setSuccessMsg(
          `✅ ${result.created} créneau(x) créé(s)${result.skipped > 0 ? `, ${result.skipped} ignoré(s) (conflits)` : ''}`
        );
        setTimeout(() => onCreated(), 1800);
      } else {
        onCreated();
      }
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
        {successMsg && <div className="alert alert-success">{successMsg}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Date</label>
            <input type="date" value={slotDate} onChange={e => setSlotDate(e.target.value)} required />
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
                    <option key={t} value={t} disabled={forbidden} style={forbidden ? { color: '#9ca3af' } : {}}>
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
                  const disabled = diff < safeConfig.slotMinHours * 60 || diff > safeConfig.slotMaxHours * 60;
                  return (
                    <option key={t} value={t} disabled={disabled} style={disabled ? { color: '#9ca3af' } : {}}>
                      {t}
                    </option>
                  );
                })}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Nombre de plongeurs (max {safeConfig.maxDivers})</label>
            <input type="number" min={2} max={safeConfig.maxDivers} value={diverCountStr}
              onChange={e => setDiverCountStr(e.target.value)}
              onBlur={() => {
                const val = parseInt(diverCountStr, 10);
                if (!diverCountStr || isNaN(val) || val < 2) setDiverCountStr('2');
                else if (val > safeConfig.maxDivers) setDiverCountStr(String(safeConfig.maxDivers));
              }} required />
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

          {/* ── Récurrence ── */}
          <div className="form-group" style={{ borderTop: '1px solid #e5e7eb', paddingTop: 12, marginTop: 4 }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', userSelect: 'none' }}>
              <input type="checkbox" checked={recurring} onChange={e => setRecurring(e.target.checked)}
                style={{ width: 16, height: 16, accentColor: '#2563eb' }} />
              <span style={{ fontWeight: 600 }}>🔁 Créneau récurrent</span>
            </label>
            <p style={{ color: '#6b7280', fontSize: 13, margin: '4px 0 0 26px' }}>
              Crée ce créneau sur les jours sélectionnés jusqu'à la date de fin (max {safeConfig.maxRecurringMonths} mois).
            </p>
          </div>

          {recurring && (
            <div style={{ background: '#f0f9ff', borderRadius: 8, padding: 14, marginTop: 4, border: '1px solid #bae6fd' }}>
              <div className="form-group" style={{ marginBottom: 12 }}>
                <label style={{ fontWeight: 600 }}>Jours de répétition</label>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 6 }}>
                  {DAYS_OF_WEEK.map(d => {
                    const selected = recurringDays.includes(d.value);
                    return (
                      <button key={d.value} type="button" onClick={() => toggleDay(d.value)}
                        style={{
                          padding: '6px 12px', borderRadius: 6, fontWeight: 600, fontSize: 13,
                          cursor: 'pointer', border: '2px solid',
                          borderColor: selected ? '#2563eb' : '#d1d5db',
                          background: selected ? '#2563eb' : '#fff',
                          color: selected ? '#fff' : '#374151',
                          transition: 'all .15s',
                        }}>
                        {d.label}
                      </button>
                    );
                  })}
                </div>
                {recurringDays.length === 0 && (
                  <p style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>Sélectionnez au moins un jour.</p>
                )}
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label style={{ fontWeight: 600 }}>Répéter jusqu'au</label>
                <input type="date" value={recurringUntil} min={slotDate} max={maxUntilDate}
                  onChange={e => setRecurringUntil(e.target.value)} required={recurring}
                  style={{ marginTop: 4 }} />
                <p style={{ color: '#6b7280', fontSize: 12, marginTop: 4 }}>
                  Maximum : {maxUntilDate} ({safeConfig.maxRecurringMonths} mois)
                </p>
              </div>
            </div>
          )}

          <div className="form-actions">
            <button type="button" className="btn btn-outline" onClick={onCancel}>Annuler</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading
                ? (recurring ? 'Création en cours...' : 'Création...')
                : (recurring ? '🔁 Créer les créneaux récurrents' : 'Créer le créneau')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
