import { useEffect, useState, useRef } from 'react';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import 'dayjs/locale/fr';
import { CalendarPicker } from '../components/CalendarPicker';
import { DayView } from '../components/DayView';
import { WeekView } from '../components/WeekView';
import { MonthView } from '../components/MonthView';
import { SlotForm } from '../components/SlotForm';
import { adminService } from '../services/adminService';
import { slotService } from '../services/slotService';
import { useAuth } from '../context/AuthContext';
import type { AppConfig } from '../types';

dayjs.extend(isoWeek);
dayjs.locale('fr');

type ViewMode = 'day' | 'week' | 'month';

function isMobile(): boolean {
  return window.innerWidth < 768;
}

export function CalendarPage({ onNavigate, returnContext, onReturnConsumed, initialDate, initialView, initialSlotId }: {
  onNavigate?: (page: string) => void;
  returnContext?: { date: string; viewMode: string } | null;
  onReturnConsumed?: () => void;
  /** Date initiale au format YYYY-MM-DD (lien direct) */
  initialDate?: string;
  /** Vue initiale : day | week | month (lien direct) */
  initialView?: 'day' | 'week' | 'month';
  /** ID du créneau à ouvrir automatiquement (lien direct) */
  initialSlotId?: number;
} = {}) {
  const { user, isAuthenticated }   = useAuth();
  const [viewMode, setViewMode]     = useState<ViewMode>(() => {
    if (initialView) return initialView;
    if (returnContext?.viewMode === 'day' || returnContext?.viewMode === 'week' || returnContext?.viewMode === 'month') return returnContext.viewMode;
    return isMobile() ? 'day' : 'week';
  });
  const [selectedDate, setSelectedDate] = useState(initialDate || returnContext?.date || dayjs().format('YYYY-MM-DD'));
  const [config, setConfig] = useState<AppConfig>({
    maxDivers: 25, slotMinHours: 1, slotMaxHours: 10, slotResolutionMinutes: 15,
    siteName: 'Carrière de Saint-Lin', slotTypes: [], clubs: [], levels: [],
    diverLevels: [], dpLevels: [], preparedLevels: [], aptitudes: [],
    publicAccess: true, selfRegistration: true,
    bookingOpenHour: -1, bookingCloseHour: -1,
    exclusiveSlotTypes: [], defaultSlotHours: 2,
    notificationBookingEmail: '', maxRecurringMonths: 4,
    notifRegistrationEnabled: true, notifApprovedEnabled: true,
    notifCancelledEnabled: true, notifMovedToWlEnabled: true, notifDpNewRegEnabled: true,
    notifSafetyReminderEnabled: false, safetyReminderDelayDays: 3, safetyReminderEmailBody: '',
    maintenanceMode: false,
  });
  // Date pour laquelle on ouvre le formulaire (null = fermé)
  const [formDate, setFormDate]           = useState<string | null>(null);
  const [formStartTime, setFormStartTime] = useState<string | undefined>(undefined);
  const [childKey, setChildKey] = useState(0); // force reload des vues
  // ID du créneau à ouvrir automatiquement (lien direct ?slot=ID)
  const [openSlotId, setOpenSlotId] = useState<number | undefined>(undefined);
  // Copie du lien : message de confirmation
  const [linkCopied, setLinkCopied] = useState(false);
  const linkCopyTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const canEdit = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'DIVE_DIRECTOR');
  const [showSidebar, setShowSidebar] = useState(false);

  useEffect(() => {
    adminService.getConfig().then(setConfig).catch(() => {});
  }, []);

  // Consommer le contexte de retour après initialisation
  useEffect(() => {
    if (returnContext) onReturnConsumed?.();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Lien direct vers un créneau : charger la date du créneau
  useEffect(() => {
    if (!initialSlotId) return;
    slotService.getById(initialSlotId).then(slot => {
      setSelectedDate(slot.slotDate);
      setViewMode('day');
      setOpenSlotId(initialSlotId);
    }).catch(() => {}); // créneau introuvable : on reste sur la date courante
  // eslint-disable-next-line react-hooks/exhaustive-deps
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
  const openForm = (date?: string, startTime?: string) => {
    const defaultDate = viewMode === 'day' ? selectedDate : dayjs().format('YYYY-MM-DD');
    setFormDate(date ?? defaultDate);
    setFormStartTime(startTime);
  };

  // Adapter pour les composants qui passent toujours une date
  const openFormWithDate = (date: string, startTime?: string) => openForm(date, startTime);

  const closeForm = () => { setFormDate(null); setFormStartTime(undefined); };

  const handleCreated = () => {
    closeForm();
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
      <button className="btn-sidebar-toggle" onClick={() => setShowSidebar(v => !v)}>
        {showSidebar ? '✕ Fermer' : '📅 Mini-calendrier & infos'}
      </button>
      <aside className={`calendar-sidebar${showSidebar ? ' sidebar-open' : ''}`}>
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
            {/* Bouton copier le lien vers la période courante */}
            <button
              className="btn btn-outline btn-small"
              title="Copier le lien vers cette période"
              onClick={() => {
                const params = new URLSearchParams({ date: selectedDate, view: viewMode });
                const url = `${window.location.origin}${window.location.pathname}?${params.toString()}`;
                navigator.clipboard.writeText(url).then(() => {
                  setLinkCopied(true);
                  if (linkCopyTimerRef.current) clearTimeout(linkCopyTimerRef.current);
                  linkCopyTimerRef.current = setTimeout(() => setLinkCopied(false), 2500);
                }).catch(() => {});
              }}
            >
              {linkCopied ? '✅ Copié !' : '🔗 Partager'}
            </button>
            {canEdit && (
              <button className="btn btn-primary btn-small" onClick={() => openForm()}>
                + Nouveau créneau
              </button>
            )}
          </div>
        </div>

        {viewMode === 'day' && (
          <DayView key={`day-${childKey}`} date={selectedDate} config={config} onAdd={openFormWithDate}
            onOpenPalanquees={onNavigate ? (id) => onNavigate(`palanquee-${id}-day`) : undefined}
            openSlotId={openSlotId}
          />
        )}
        {viewMode === 'week' && (
          <WeekView key={`week-${childKey}`} weekStart={weekStart} config={config}
            onSelectDay={d => { setSelectedDate(d); setViewMode('day'); }}
            onAdd={openFormWithDate}
            onOpenPalanquees={onNavigate ? (id) => onNavigate(`palanquee-${id}-week`) : undefined}
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
            initialStartTime={formStartTime}
            onCreated={handleCreated}
            onCancel={closeForm}
            isAdmin={user?.role === 'ADMIN'}
          />
        )}
      </main>
    </div>
  );
}

