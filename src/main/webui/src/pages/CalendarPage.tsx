import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import 'dayjs/locale/fr';
import { CalendarPicker } from '../components/CalendarPicker';
import { DayView } from '../components/DayView';
import { WeekView } from '../components/WeekView';
import { adminService } from '../services/adminService';
import type { AppConfig } from '../types';

dayjs.extend(isoWeek);
dayjs.locale('fr');

export function CalendarPage() {
  const [viewMode, setViewMode] = useState<'day' | 'week'>('day');
  const [selectedDate, setSelectedDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [config, setConfig] = useState<AppConfig>({ maxDivers: 25, slotMinHours: 1, slotMaxHours: 10, slotResolutionMinutes: 15 });

  useEffect(() => {
    adminService.getConfig().then(setConfig).catch(() => {});
  }, []);

  const weekStart = dayjs(selectedDate).startOf('isoWeek').format('YYYY-MM-DD');

  const prevPeriod = () => {
    if (viewMode === 'day') setSelectedDate(d => dayjs(d).subtract(1, 'day').format('YYYY-MM-DD'));
    else setSelectedDate(d => dayjs(d).subtract(7, 'day').format('YYYY-MM-DD'));
  };
  const nextPeriod = () => {
    if (viewMode === 'day') setSelectedDate(d => dayjs(d).add(1, 'day').format('YYYY-MM-DD'));
    else setSelectedDate(d => dayjs(d).add(7, 'day').format('YYYY-MM-DD'));
  };

  return (
    <div className="calendar-page">
      <aside className="calendar-sidebar">
        <CalendarPicker selectedDate={selectedDate} onDateChange={setSelectedDate} />
        <div className="config-info">
          <h4>📊 Capacité du lac</h4>
          <p>Maximum : <strong>{config.maxDivers} plongeurs</strong></p>
          <p>Durée min : <strong>{config.slotMinHours}h</strong></p>
          <p>Durée max : <strong>{config.slotMaxHours}h</strong></p>
          <p>Résolution : <strong>{config.slotResolutionMinutes} min</strong></p>
        </div>
      </aside>

      <main className="calendar-main">
        <div className="view-controls">
          <div className="view-nav">
            <button className="btn btn-outline" onClick={prevPeriod}>‹ Précédent</button>
            <button className="btn btn-outline" onClick={() => setSelectedDate(dayjs().format('YYYY-MM-DD'))}>
              Aujourd'hui
            </button>
            <button className="btn btn-outline" onClick={nextPeriod}>Suivant ›</button>
          </div>
          <div className="view-toggle">
            <button
              className={`btn ${viewMode === 'day' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setViewMode('day')}
            >
              📅 Jour
            </button>
            <button
              className={`btn ${viewMode === 'week' ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setViewMode('week')}
            >
              🗓️ Semaine
            </button>
          </div>
        </div>

        {viewMode === 'day' ? (
          <DayView date={selectedDate} config={config} />
        ) : (
          <WeekView
            weekStart={weekStart}
            config={config}
            onSelectDay={(d) => { setSelectedDate(d); setViewMode('day'); }}
          />
        )}
      </main>
    </div>
  );
}

