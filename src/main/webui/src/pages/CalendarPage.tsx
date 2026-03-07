import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import 'dayjs/locale/fr';
import { CalendarPicker } from '../components/CalendarPicker';
import { DayView } from '../components/DayView';
import { WeekView } from '../components/WeekView';
import { MonthView } from '../components/MonthView';
import { SlotForm } from '../components/SlotForm';
import { adminService } from '../services/adminService';
import { useAuth } from '../context/AuthContext';
import type { AppConfig } from '../types';

dayjs.extend(isoWeek);
dayjs.locale('fr');

type ViewMode = 'day' | 'week' | 'month';

export function CalendarPage() {
  const { user, isAuthenticated }   = useAuth();
  const [viewMode, setViewMode]     = useState<ViewMode>('week');
  const [selectedDate, setSelectedDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [config, setConfig] = useState<AppConfig>({
    maxDivers: 25, slotMinHours: 1, slotMaxHours: 10, slotResolutionMinutes: 15,
    siteName: 'Carri\u00e8re de Saint-Lin', slotTypes: [], clubs: [],
    publicAccess: true, selfRegistration: true,
  });
  // Date pour laquelle on ouvre le formulaire (null = fermé)
  const [formDate, setFormDate] = useState<string | null>(null);
  const [childKey, setChildKey] = useState(0); // force reload des vues

  const canEdit = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');

  useEffect(() => {
    adminService.getConfig().then(setConfig).catch(() => {});
  }, []);

  const weekStart = dayjs(selectedDate).startOf('isoWeek').format('YYYY-MM-DD');
  const selDay    = dayjs(selectedDate);
  const year      = selDay.year();
  const month     = selDay.month() + 1;

  const prevPeriod = () => {
    if (viewMode === 'day')   setSelectedDate(d => dayjs(d).subtract(1, 'day').format('YYYY-MM-DD'));
    if (viewMode === 'week')  setSelectedDate(d => dayjs(d).subtract(1, 'week').format('YYYY-MM-DD'));
    if (viewMode === 'month') setSelectedDate(d => dayjs(d).subtract(1, 'month').format('YYYY-MM-DD'));
  };
  const nextPeriod = () => {
    if (viewMode === 'day')   setSelectedDate(d => dayjs(d).add(1, 'day').format('YYYY-MM-DD'));
    if (viewMode === 'week')  setSelectedDate(d => dayjs(d).add(1, 'week').format('YYYY-MM-DD'));
    if (viewMode === 'month') setSelectedDate(d => dayjs(d).add(1, 'month').format('YYYY-MM-DD'));
  };

  // Ouvrir le formulaire avec la date précisée, ou la date courante par défaut
  const openForm = (date?: string) => {
    const defaultDate = viewMode === 'day' ? selectedDate : dayjs().format('YYYY-MM-DD');
    setFormDate(date ?? defaultDate);
  };

  // Adapter pour les composants qui passent toujours une date
  const openFormWithDate = (date: string) => openForm(date);

  const handleCreated = () => {
    setFormDate(null);
    setChildKey(k => k + 1); // force rechargement des vues
  };

  const periodLabel = (() => {
    if (viewMode === 'day')   return selDay.locale('fr').format('dddd D MMMM YYYY');
    if (viewMode === 'week') {
      const ws = dayjs(weekStart);
      const we = ws.add(6, 'day');
      return `${ws.format('D MMM')} – ${we.locale('fr').format('D MMM YYYY')}`;
    }
    return selDay.locale('fr').format('MMMM YYYY');
  })();

  return (
    <div className="calendar-page">
      <aside className="calendar-sidebar">
        <CalendarPicker selectedDate={selectedDate} onDateChange={setSelectedDate} />
        <div className="config-info">
          <h4>📊 Capacité</h4>
          <p>Maximum : <strong>{config.maxDivers} plongeurs</strong></p>
          <p>Durée min d'un créneau : <strong>{config.slotMinHours}h</strong></p>
          <p>Durée max : <strong>{config.slotMaxHours}h</strong></p>
        </div>
      </aside>

      <main className="calendar-main">
        <div className="view-controls">
          {/* Navigation */}
          <div className="view-nav">
            <button className="btn btn-outline btn-small" onClick={prevPeriod}>‹</button>
            <button className="btn btn-outline btn-small"
              onClick={() => setSelectedDate(dayjs().format('YYYY-MM-DD'))}>
              Aujourd'hui
            </button>
            <button className="btn btn-outline btn-small" onClick={nextPeriod}>›</button>
            <span className="period-label">{periodLabel}</span>
          </div>

          {/* Sélecteur de vue + bouton ajout */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <div className="view-toggle">
              <button className={`btn btn-small ${viewMode === 'day'   ? 'btn-primary' : 'btn-outline'}`} onClick={() => setViewMode('day')}>📅 Jour</button>
              <button className={`btn btn-small ${viewMode === 'week'  ? 'btn-primary' : 'btn-outline'}`} onClick={() => setViewMode('week')}>🗓️ Semaine</button>
              <button className={`btn btn-small ${viewMode === 'month' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setViewMode('month')}>📆 Mois</button>
            </div>
            {canEdit && (
              <button className="btn btn-primary btn-small" onClick={() => openForm()}>
                + Nouveau créneau
              </button>
            )}
          </div>
        </div>

        {viewMode === 'day' && (
          <DayView key={`day-${childKey}`} date={selectedDate} config={config} onAdd={openFormWithDate} />
        )}
        {viewMode === 'week' && (
          <WeekView key={`week-${childKey}`} weekStart={weekStart} config={config}
            onSelectDay={d => { setSelectedDate(d); setViewMode('day'); }}
            onAdd={openFormWithDate}
          />
        )}
        {viewMode === 'month' && (
          <MonthView key={`month-${childKey}`} year={year} month={month}
            onSelectDay={d => { setSelectedDate(d); setViewMode('day'); }}
            onAdd={openFormWithDate}
          />
        )}

        {/* Formulaire global — date par défaut = jour courant ou date sélectionnée */}
        {formDate && canEdit && (
          <SlotForm
            date={formDate}
            config={config}
            onCreated={handleCreated}
            onCancel={() => setFormDate(null)}
          />
        )}
      </main>
    </div>
  );
}

